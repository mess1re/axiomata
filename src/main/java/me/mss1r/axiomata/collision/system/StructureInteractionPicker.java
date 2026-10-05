package me.mss1r.axiomata.collision.system;

import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.StructureCollisionRegistry;
import me.mss1r.axiomata.blueprint.api.construction.ConstructionHitTesting;
import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;
import java.util.LinkedHashSet;
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

        var candidates = new LinkedHashSet<>(StructureCollisionSystem.structuresNear(viewer, path));
        // Loading poses can extend beyond the authored construction pose, or vice versa.
        for (CollidableStructure structure : StructureCollisionRegistry.snapshot()) {
            if (structure instanceof UnderConstruction machine && !machine.isFullyBuilt()) {
                candidates.add(structure);
            }
        }
        for (CollidableStructure structure : candidates) {
            if (!(structure instanceof Entity entity) || entity == viewer || entity.isRemoved()
                    || entity.level() != viewer.level() || viewer.isPassengerOfSameVehicle(entity)
                    || !targetPredicate.test(entity)) {
                continue;
            }
            Optional<Vec3> hit = structure instanceof UnderConstruction machine && !machine.isFullyBuilt()
                    ? ConstructionHitTesting.clip(structure, machine, start, end)
                    : StructureCollisionResolver.clipSegment(structure, start, end);
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
        if (structure instanceof UnderConstruction machine && !machine.isFullyBuilt()) {
            Vec3 start = viewer.getEyePosition();
            Optional<Vec3> hit = ConstructionHitTesting.clip(structure, machine, start,
                    start.add(viewer.getViewVector(1).scale(nonNegativeReach)));
            return hit.isPresent();
        }
        return StructureCollisionResolver.distanceToSqr(structure, viewer.getEyePosition())
                < nonNegativeReach * nonNegativeReach;
    }
}
