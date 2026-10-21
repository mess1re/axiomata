package me.mss1r.axiomata.blueprint.api.construction;

import me.mss1r.axiomata.collision.CollidableStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Where a build is placed: on a block, or on the surface of a finished structure such as a ship's deck, which then
 * carries the build with it.
 */
public record ConstructionTarget(@Nullable BlockHitResult block, @Nullable CollidableStructure structure,
                                 Vec3 location) {
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
