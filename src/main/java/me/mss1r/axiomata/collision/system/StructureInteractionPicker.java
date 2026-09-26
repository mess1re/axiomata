package me.mss1r.axiomata.collision.system;

import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class StructureInteractionPicker {
    private static final double PATH_MARGIN = 0.25D;

    private StructureInteractionPicker() {
    }

    @Nullable
    public static EntityHitResult findHit(Entity viewer, Vec3 start, Vec3 end,
                                          Predicate<Entity> targetPredicate, double maxDistanceSqr) {
        AABB path = new AABB(start, end).inflate(PATH_MARGIN);
        Entity closestEntity = null;
        Vec3 closestHit = null;
        double closestDistanceSqr = maxDistanceSqr;

        for (CollidableStructure structure : StructureCollisionSystem.structuresNear(viewer, path)) {
            if (!(structure instanceof Entity entity) || !targetPredicate.test(entity)) {
                continue;
            }
            Optional<Vec3> hit = StructureCollisionResolver.clipSegment(structure, start, end);
            if (hit.isEmpty()) {
                continue;
            }
            double distanceSqr = start.distanceToSqr(hit.get());
            if (distanceSqr < closestDistanceSqr) {
                closestEntity = entity;
                closestHit = hit.get();
                closestDistanceSqr = distanceSqr;
            }
        }

        return closestEntity == null ? null : new EntityHitResult(closestEntity, closestHit);
    }

    public static boolean isWithinReach(Entity viewer, CollidableStructure structure, double reach) {
        double nonNegativeReach = Math.max(0.0D, reach);
        return StructureCollisionResolver.distanceToSqr(structure, viewer.getEyePosition())
                < nonNegativeReach * nonNegativeReach;
    }
}
