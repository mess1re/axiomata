package me.mss1r.axiomata.collision;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public final class StructurePlacement {
    private static final double CLEARANCE = 0.01D;

    private StructurePlacement() {
    }

    public record Result(StructureTransform transform, CollisionGroup support) {
    }

    /**
     * Lowers the candidate onto the highest solid surface under its footprint.
     * Only queries geometry; the consumer must validate world clearance and spawn the entity.
     */
    public static Optional<Result> findOnTop(CollidableStructure candidate, CollidableStructure support,
                                              double x, double z, float yawDegrees) {
        if (candidate == support || !Double.isFinite(x) || !Double.isFinite(z) || !Float.isFinite(yawDegrees)) {
            return Optional.empty();
        }
        List<CollisionGroup> moving = candidate.solidCollisionGroups();
        List<CollisionGroup> fixed = support.solidCollisionGroups();
        StructureTransform origin = new StructureTransform(x, 0, z, yawDegrees);
        AABB candidateBounds = StructureCollisionResolver.worldBounds(origin, moving);
        AABB supportBounds = StructureCollisionResolver.worldBounds(support.collisionTransform(), fixed);
        if (candidateBounds == null || supportBounds == null) return Optional.empty();
        double startY = supportBounds.maxY - candidateBounds.minY + CLEARANCE;
        StructureTransform start = new StructureTransform(x, startY, z, yawDegrees);
        Vec3 down = new Vec3(0, -(supportBounds.getYsize() + candidateBounds.getYsize() + 2 * CLEARANCE), 0);
        StructureCollisionResolver.ResolvedMovement response = StructureCollisionResolver.resolveStructureMovement(
                start, moving, support.collisionTransform(), fixed, down);
        if (!response.isSupported()) return Optional.empty();
        return Optional.of(new Result(new StructureTransform(x, startY + response.allowed().y, z, yawDegrees), response.groundGroup()));
    }
}
