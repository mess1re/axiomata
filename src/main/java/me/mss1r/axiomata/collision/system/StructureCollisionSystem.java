package me.mss1r.axiomata.collision.system;

import java.util.List;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.StructureGeometry;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.StructureCollisionRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class StructureCollisionSystem {
    /** Upper bound for {@link CollidableStructure#walkStepHeight()}: steps, never a wall. */
    private static final double MAX_STRUCTURE_STEP = 1.0D;
    private static final double BOUNDS_MARGIN = 0.25D;
    private static final double ASCENDING_EPSILON = 1.0E-4D;

    private StructureCollisionSystem() {
    }

    public record Movement(Vec3 allowed, boolean supported) {
    }

    public static void register(CollidableStructure structure) {
        StructureCollisionRegistry.register(structure);
    }

    public static void unregister(CollidableStructure structure) {
        StructureCollisionRegistry.unregister(structure);
    }

    public static AABB worldBounds(CollidableStructure structure) {
        return StructureCollisionRegistry.worldBounds(structure);
    }

    public static Movement collideMovement(Entity entity, Vec3 vanillaMovement) {
        return collideMovement(entity, vanillaMovement, vanillaMovement);
    }

    public static Movement collideMovement(Entity entity, Vec3 requestedMovement, Vec3 vanillaMovement) {
        // Hybrid consumers keep vanilla movement for their wheel/body box, even though they
        // expose a separate articulated structure. That box still needs to hit other structures.
        if (!canCollide(entity) && !(entity.isAlive() && !entity.noPhysics
                && entity instanceof CollidableStructure structure && !structure.movesByGeometry())) {
            return new Movement(vanillaMovement, false);
        }

        AABB box = entity.getBoundingBox();
        double height = entity.maxUpStep();
        AABB path = box.expandTowards(requestedMovement)
                .expandTowards(0, Math.max(height, MAX_STRUCTURE_STEP), 0).inflate(BOUNDS_MARGIN);
        List<CollidableStructure> nearby = structuresNear(entity, path).stream()
                .filter(structure -> !StructureClimbingSystem.isGuidedClimbing(entity, structure)).toList();
        if (nearby.isEmpty()) return new Movement(vanillaMovement, false);
        if (entity instanceof CollidableStructure structure) {
            // A load on this body is part of its assembly, not an obstacle to the native box.
            SupportedStructures assembly = SupportedStructures.of((Entity & CollidableStructure) entity,
                    StructureGeometry.solid(structure), other -> false);
            nearby = nearby.stream().filter(other -> !assembly.contains(other)).toList();
        }
        if (height > 0) {
            for (CollidableStructure structure : nearby) {
                height = Math.max(height, Math.min(MAX_STRUCTURE_STEP, structure.walkStepHeight()));
            }
        }
        Movement result = resolve(entity, box, vanillaMovement, nearby);
        boolean grounded = entity.onGround() || requestedMovement.y < 0 && result.supported();
        if (height > 0 && grounded && requestedMovement.y <= 0
                && result.allowed().horizontalDistanceSqr() < requestedMovement.horizontalDistanceSqr() - 1.0E-8D) {
            // Match vanilla's up/across/down step, checking both world and structure geometry.
            Movement up = resolve(entity, box, new Vec3(0, height, 0), nearby);
            AABB raised = box.move(up.allowed());
            Movement across = resolve(entity, raised, new Vec3(requestedMovement.x, 0, requestedMovement.z), nearby);
            Movement down = resolve(entity, raised.move(across.allowed()),
                    new Vec3(0, requestedMovement.y - up.allowed().y, 0), nearby);
            Vec3 stepped = up.allowed().add(across.allowed()).add(down.allowed());
            if (stepped.horizontalDistanceSqr() > result.allowed().horizontalDistanceSqr() + 1.0E-8D) {
                return new Movement(stepped, down.supported());
            }
        }
        return new Movement(result.allowed(), result.supported()
                && requestedMovement.y <= ASCENDING_EPSILON && vanillaMovement.y <= ASCENDING_EPSILON);
    }

    private static Movement resolve(Entity entity, AABB box, Vec3 wanted, List<CollidableStructure> nearby) {
        Vec3 allowed = collideWithWorld(entity, box, wanted);
        boolean supported = false;
        for (int pass = 0; pass < 4; pass++) {
            Vec3 before = allowed;
            for (CollidableStructure structure : nearby) {
                var response = StructureCollisionResolver.resolveMovement(structure, box, allowed, 0.0D);
                supported |= response.isSupported() && wanted.y <= ASCENDING_EPSILON;
                allowed = response.allowed();
            }
            // Untouched by the structures, the movement is already the world's answer.
            if (before.distanceToSqr(allowed) < 1.0E-16D) break;
            allowed = collideWithWorld(entity, box, allowed);
        }
        return new Movement(allowed, supported);
    }

    public static void markSupported(Entity entity) {
        if (entity.getDeltaMovement().y > ASCENDING_EPSILON) {
            return;
        }
        entity.setOnGround(true);
        entity.fallDistance = 0.0F;
        VirtualPlatformSupport.markSupported(entity);
    }

    public static List<CollidableStructure> structuresNear(Entity movingEntity, AABB path) {
        return StructureCollisionRegistry.structuresNear(movingEntity, path, BOUNDS_MARGIN);
    }

    static boolean canCollide(Entity entity) {
        return entity.isAlive() && !entity.noPhysics && !(entity instanceof CollidableStructure)
                && !(entity instanceof Player player && player.isSpectator());
    }

    static Vec3 collideWithWorld(Entity entity, Vec3 requested) {
        return collideWithWorld(entity, entity.getBoundingBox(), requested);
    }

    private static Vec3 collideWithWorld(Entity entity, AABB box, Vec3 requested) {
        List<VoxelShape> entityCollisions = entity.level().getEntityCollisions(
                entity, box.expandTowards(requested));
        return Entity.collideBoundingBox(entity, requested, box, entity.level(), entityCollisions);
    }

}
