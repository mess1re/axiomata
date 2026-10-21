package me.mss1r.axiomata.blueprint.api.construction;

import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.StructureCollisionRegistry;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.StructureGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Where a build is placed: on a block, or on the surface of a finished structure such as a ship's deck, which then
 * carries the build with it.
 */
public record ConstructionTarget(@Nullable BlockHitResult block, @Nullable CollidableStructure structure,
                                 Vec3 location) {
    public ConstructionTarget {
        if ((block == null) == (structure == null) || location == null
                || !Double.isFinite(location.x) || !Double.isFinite(location.y) || !Double.isFinite(location.z)) {
            throw new IllegalArgumentException("A placement target needs one surface and a finite hit location");
        }
    }

    /**
     * Shared aim query for blueprints and deployment items. Solid, finished structures occlude
     * blocks behind them; decorative rigging and unfinished sites are not placement surfaces.
     * Fluid placement remains block-only. Call again on the server rather than trusting a client hit.
     */
    @Nullable
    public static ConstructionTarget pick(Player player, double reach, float partialTick, boolean fluids) {
        if (!Double.isFinite(reach) || reach <= 0) return null;
        HitResult hit = player.pick(reach, partialTick, fluids);
        ConstructionTarget closest = hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
                ? of(blockHit) : null;
        if (fluids) return closest;
        Vec3 start = player.getEyePosition(partialTick);
        Vec3 end = start.add(player.getViewVector(partialTick).scale(reach));
        double distance = closest == null ? reach * reach : start.distanceToSqr(closest.location());
        for (CollidableStructure candidate : StructureCollisionRegistry.structuresNear(player, new AABB(start, end), .01D)) {
            if (!(candidate instanceof Entity entity) || entity.isRemoved()
                    || candidate instanceof UnderConstruction machine && !machine.isFullyBuilt()) continue;
            var point = StructureCollisionResolver.clipSegment(candidate.collisionTransform(),
                    StructureGeometry.solid(candidate), start, end, 0);
            if (point.isPresent() && start.distanceToSqr(point.get()) < distance) {
                distance = start.distanceToSqr(point.get());
                closest = on(candidate, point.get());
            }
        }
        return closest;
    }

    public static ConstructionTarget of(BlockHitResult block) {
        return new ConstructionTarget(block, null, block.getLocation());
    }

    public static ConstructionTarget on(CollidableStructure structure, Vec3 location) {
        return new ConstructionTarget(null, structure, location);
    }

    public BlockPos pos() {
        return block != null ? block.getBlockPos() : BlockPos.containing(location);
    }

    public Direction face() {
        return block != null ? block.getDirection() : Direction.UP;
    }
}
