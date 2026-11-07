package me.mss1r.axiomata.ballistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Vanilla block traversal with context-free collider shapes, for physical traces without an entity. */
public final class BlockClip {
    private BlockClip() {}

    public static BlockHitResult colliders(BlockGetter level, Vec3 from, Vec3 to) {
        return BlockGetter.traverseBlocks(from, to, level, (world, pos) -> {
            var state = world.getBlockState(pos);
            var shape = state.getCollisionShape(world, pos, CollisionContext.empty());
            return world.clipWithInteractionOverride(from, to, pos, shape, state);
        }, world -> {
            Vec3 back = from.subtract(to);
            return BlockHitResult.miss(to, Direction.getNearest(back.x, back.y, back.z), BlockPos.containing(to));
        });
    }
}
