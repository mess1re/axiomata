package me.mss1r.axiomata.collision;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public final class StructurePlacement {
    private static final double CLEARANCE = 0.01D;
    /** How far above the aimed point the candidate starts, so a slightly raised plank still takes it. */
    private static final double STEP = 0.6D;

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
        return findOnTop(candidate, support, x, Double.NaN, z, yawDegrees);
    }

    /**
     * Lowers the candidate from just above {@code fromY} onto the first solid surface under its footprint, e.g. the
     * deck a player aims at rather than the top of a mast above it. {@code NaN} starts above the whole support.
     */
    public static Optional<Result> findOnTop(CollidableStructure candidate, CollidableStructure support,
                                              double x, double fromY, double z, float yawDegrees) {
        if (candidate == support || !Double.isFinite(x) || !Double.isFinite(z) || !Float.isFinite(yawDegrees)) {
            return Optional.empty();
        }
        List<CollisionGroup> moving = StructureGeometry.solid(candidate);
        List<CollisionGroup> fixed = StructureGeometry.solid(support);
        StructureTransform origin = new StructureTransform(x, 0, z, yawDegrees);
        AABB candidateBounds = StructureCollisionResolver.worldBounds(origin, moving);
        AABB supportBounds = StructureCollisionResolver.worldBounds(support.collisionTransform(), fixed);
        if (candidateBounds == null || supportBounds == null) return Optional.empty();
        double top = Double.isFinite(fromY) ? Math.min(supportBounds.maxY, fromY + STEP) : supportBounds.maxY;
        double startY = top - candidateBounds.minY + CLEARANCE;
        StructureTransform start = new StructureTransform(x, startY, z, yawDegrees);
        Vec3 down = new Vec3(0, -(supportBounds.getYsize() + candidateBounds.getYsize() + 2 * CLEARANCE), 0);
        StructureCollisionResolver.ResolvedMovement response = StructureCollisionResolver.resolveStructureMovement(
                start, moving, support.collisionTransform(), fixed, down);
        if (!response.isSupported()) return Optional.empty();
        return Optional.of(new Result(new StructureTransform(x, startY + response.allowed().y, z, yawDegrees), response.groundGroup()));
    }
}
