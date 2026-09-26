package me.mss1r.axiomata.collision.system;

import java.util.List;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.StructureCollisionRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class StructureCollisionSystem {
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
        if (!canCollide(entity)) {
            return new Movement(vanillaMovement, false);
        }

        AABB path = entity.getBoundingBox().expandTowards(vanillaMovement).inflate(BOUNDS_MARGIN);
        Vec3 allowed = vanillaMovement;
        boolean supported = false;
        boolean changed = false;

        for (CollidableStructure structure : structuresNear(entity, path)) {
            // Climbing already supplied a centred, guided movement this tick. Resolving against
            // the same structure again would stop the climber on the opening around the ladder.
            if (StructureClimbingSystem.isGuidedClimbing(entity, structure)) {
                continue;
            }
            StructureCollisionResolver.ResolvedMovement response = StructureCollisionResolver.resolveMovement(
                    structure, entity.getBoundingBox(), allowed, 0.0D);
            supported |= response.isSupported()
                    && requestedMovement.y <= ASCENDING_EPSILON
                    && vanillaMovement.y <= ASCENDING_EPSILON;
            if (!response.allowed().equals(allowed)) {
                allowed = response.allowed();
                changed = true;
            }
        }

        return new Movement(changed ? collideWithWorld(entity, allowed) : allowed, supported);
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
        AABB box = entity.getBoundingBox();
        List<VoxelShape> entityCollisions = entity.level().getEntityCollisions(
                entity, box.expandTowards(requested));
        return Entity.collideBoundingBox(entity, requested, box, entity.level(), entityCollisions);
    }

}
