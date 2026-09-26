package me.mss1r.axiomata.collision.system;

import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPose;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.StructureTransform;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class StructureTerrainCollision {
    private static final double MOVEMENT_EPSILON = 1.0E-8D;
    private static final double STEP_MARGIN = 1.0E-4D;
    private static final double MINIMUM_ESCAPE = 1.0E-3D;
    private static final double PENETRATION_EPSILON = 1.0E-8D;
    private static final double MAX_UNSTICK_STEP = 0.15D;
    private static final int MAX_OBSTACLES = 768;

    private StructureTerrainCollision() {
    }

    public static <T extends Entity & CollidableStructure> Vec3 clampMovement(
            T structure, Vec3 wanted, double stepHeight, Predicate<CollisionGroup> excluded) {
        Vec3 horizontal = new Vec3(wanted.x, 0.0D, wanted.z);
        if (horizontal.lengthSqr() <= MOVEMENT_EPSILON) {
            return wanted;
        }
        List<CollisionGroup> groups = terrainGroups(structure, excluded);
        if (groups.isEmpty()) {
            return wanted;
        }
        StructureTransform transform = structure.collisionTransform();
        AABB bounds = StructureCollisionResolver.worldBounds(transform, groups);
        if (bounds == null) {
            return wanted;
        }
        List<AABB> obstacles = collectObstacles(
                structure, bounds.expandTowards(horizontal).inflate(0.5D), stepHeight);
        if (obstacles.isEmpty()) {
            return wanted;
        }

        Vec3 allowed = StructureCollisionResolver
                .resolveStructureMovement(transform, groups, obstacles, horizontal).allowed();
        return new Vec3(allowed.x, wanted.y, allowed.z);
    }

    public static <T extends Entity & CollidableStructure> boolean canOccupy(
            T structure, StructureTransform current, StructureTransform wanted,
            double stepHeight, Predicate<CollisionGroup> excluded) {
        List<CollisionGroup> groups = terrainGroups(structure, excluded);
        if (groups.isEmpty()) {
            return true;
        }
        AABB wantedBounds = StructureCollisionResolver.worldBounds(wanted, groups);
        AABB currentBounds = StructureCollisionResolver.worldBounds(current, groups);
        if (wantedBounds == null || currentBounds == null) {
            return true;
        }
        AABB search = new AABB(
                Math.min(currentBounds.minX, wantedBounds.minX),
                Math.min(currentBounds.minY, wantedBounds.minY),
                Math.min(currentBounds.minZ, wantedBounds.minZ),
                Math.max(currentBounds.maxX, wantedBounds.maxX),
                Math.max(currentBounds.maxY, wantedBounds.maxY),
                Math.max(currentBounds.maxZ, wantedBounds.maxZ));
        List<AABB> obstacles = collectObstacles(structure, search.inflate(0.5D), stepHeight);
        if (obstacles.isEmpty()) {
            return true;
        }
        double wantedPenetration = penetration(wanted, groups, obstacles);
        if (wantedPenetration <= PENETRATION_EPSILON) {
            return true;
        }
        double currentPenetration = penetration(current, groups, obstacles);
        return wantedPenetration + PENETRATION_EPSILON < currentPenetration;
    }

    public static <T extends Entity & CollidableStructure> void unstick(
            T structure, double stepHeight, Predicate<CollisionGroup> excluded) {
        List<CollisionGroup> groups = terrainGroups(structure, excluded);
        if (groups.isEmpty()) {
            return;
        }
        StructureTransform transform = structure.collisionTransform();
        AABB bounds = StructureCollisionResolver.worldBounds(transform, groups);
        if (bounds == null) {
            return;
        }
        List<AABB> obstacles = collectObstacles(structure, bounds.inflate(0.5D), stepHeight);
        if (obstacles.isEmpty()) {
            return;
        }

        Vec3 deepest = Vec3.ZERO;
        double deepestLength = 0.0D;
        for (AABB obstacle : obstacles) {
            Vec3 push = StructureCollisionResolver
                    .resolve(transform, groups, obstacle, Vec3.ZERO, 0.0D).pushOut();
            double length = push.lengthSqr();
            if (length > deepestLength) {
                deepestLength = length;
                deepest = push;
            }
        }
        if (deepestLength <= MINIMUM_ESCAPE * MINIMUM_ESCAPE) {
            return;
        }

        Vec3 escape = deepest.reverse();
        double length = Math.sqrt(deepestLength);
        if (length > MAX_UNSTICK_STEP) {
            escape = escape.scale(MAX_UNSTICK_STEP / length);
        }
        structure.setPos(structure.getX() + escape.x, structure.getY() + escape.y, structure.getZ() + escape.z);
    }

    private static double penetration(StructureTransform transform, List<CollisionGroup> groups,
                                      List<AABB> obstacles) {
        double total = 0.0D;
        for (AABB obstacle : obstacles) {
            Vec3 push = StructureCollisionResolver
                    .resolve(transform, groups, obstacle, Vec3.ZERO, 0.0D).pushOut();
            total += push.lengthSqr();
        }
        return total;
    }

    private static List<CollisionGroup> terrainGroups(CollidableStructure structure,
                                                      Predicate<CollisionGroup> excluded) {
        List<CollisionGroup> current = structure.solidCollisionGroups();
        if (current.isEmpty()) {
            return List.of();
        }
        List<CollisionGroup> previous = structure.previousSolidCollisionGroups();
        List<CollisionGroup> still = new ArrayList<>(current.size());
        for (CollisionGroup group : current) {
            if (excluded.test(group)) {
                continue;
            }
            CollisionPose before = poseOf(previous, group.name());
            if (before == null || before.equals(group.pose())) {
                still.add(group);
            }
        }
        return still;
    }

    private static CollisionPose poseOf(List<CollisionGroup> groups, String name) {
        for (CollisionGroup group : groups) {
            if (group.name().equals(name)) {
                return group.pose();
            }
        }
        return null;
    }

    private static List<AABB> collectObstacles(Entity structure, AABB search, double stepHeight) {
        double steppableTop = structure.getY() + Math.max(0.0D, stepHeight) + STEP_MARGIN;
        List<AABB> obstacles = new ArrayList<>();
        for (VoxelShape shape : structure.level().getBlockCollisions(structure, search)) {
            for (AABB box : shape.toAabbs()) {
                if (box.maxY <= steppableTop) {
                    continue;
                }
                obstacles.add(box);
                if (obstacles.size() >= MAX_OBSTACLES) {
                    return obstacles;
                }
            }
        }
        return obstacles;
    }
}
