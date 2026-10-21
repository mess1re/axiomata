package me.mss1r.axiomata.collision;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;

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
        if (candidate == support || !Double.isFinite(x) || !Double.isFinite(z) || !Float.isFinite(yawDegrees)
                || Double.isInfinite(fromY)
                || support instanceof UnderConstruction machine && !machine.isFullyBuilt()) {
            return Optional.empty();
        }
        List<CollisionGroup> moving = StructureGeometry.supportContact(candidate);
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
        StructureTransform placed = new StructureTransform(x, startY + response.allowed().y, z, yawDegrees);
        // A sweep can start inside a mast or a low ceiling. Contact alone is not proof of clearance.
        if (overlaps(support.collisionTransform(), fixed, placed, moving)) return Optional.empty();
        return Optional.of(new Result(placed, response.groundGroup()));
    }

    /**
     * Checks the complete candidate geometry against blocks, the world border and other structures.
     * Unfinished sites reserve their planned geometry, but remain non-solid during normal play.
     * Does not move the candidate or add it to the level; consumers spend items only after spawning.
     */
    public static <T extends Entity & CollidableStructure> boolean isClear(T candidate, StructureTransform at) {
        List<CollisionGroup> groups = StructureGeometry.supportContact(candidate);
        AABB bounds = StructureCollisionResolver.worldBounds(at, groups);
        if (bounds == null || !candidate.level().getWorldBorder().isWithinBounds(bounds)) return false;
        // Query occupied parts, not the potentially enormous empty volume between them.
        for (CollisionGroup group : groups) {
            for (CollisionPart part : group.parts()) {
                AABB partBounds = StructureCollisionResolver.worldPartBounds(at, group, part);
                for (var shape : candidate.level().getBlockCollisions(candidate, partBounds)) {
                    for (AABB obstacle : shape.toAabbs()) {
                        if (StructureCollisionResolver.resolve(at, groups, obstacle, Vec3.ZERO, 0)
                                .pushOut().lengthSqr() > 1.0E-8D) return false;
                    }
                }
            }
        }
        for (CollidableStructure other : StructureCollisionRegistry.structuresNear(candidate, bounds, .01D)) {
            if (overlaps(other.collisionTransform(), StructureGeometry.supportContact(other), at, groups)) return false;
        }
        return true;
    }

    private static boolean overlaps(StructureTransform fixed, List<CollisionGroup> fixedGroups,
                                     StructureTransform moving, List<CollisionGroup> movingGroups) {
        return StructureCollisionResolver.resolveStructures(fixed, fixedGroups, moving, movingGroups, Vec3.ZERO)
                .pushOut().lengthSqr() > 1.0E-8D;
    }
}
