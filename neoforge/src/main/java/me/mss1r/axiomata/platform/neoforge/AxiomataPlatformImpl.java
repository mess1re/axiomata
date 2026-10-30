package me.mss1r.axiomata.platform.neoforge;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.entity.PartEntity;

public final class AxiomataPlatformImpl {
    public static net.minecraft.nbt.CompoundTag persistentData(Entity entity) { return entity.getPersistentData(); }
    public static boolean canReach(net.minecraft.server.level.ServerPlayer player, Entity entity, double padding) { return player.canInteractWithEntity(entity, padding); }
    public static double entityReach(Player player) { return player.entityInteractionRange(); }
    public static double blockReach(Player player) { return player.blockInteractionRange(); }
    public static Entity logicalTarget(Entity entity) { return entity instanceof PartEntity<?> part ? part.getParent() : entity; }
    public static boolean blocksWithShield(ItemStack stack) { return stack.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SHIELD_BLOCK); }
    public static boolean breakCancelled(ServerLevel level, BlockPos pos, BlockState state, Player breaker) {
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, breaker);
        NeoForge.EVENT_BUS.post(event);
        return event.isCanceled();
    }

    public static boolean placeProtectedBlock(ServerLevel level, BlockPos pos, BlockState placed, Player breaker) {
        BlockState previous = level.getBlockState(pos);
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        boolean accepted = false;
        try {
            // Hold back neighbours and client updates until the placement is accepted.
            if (!level.setBlock(pos, placed, Block.UPDATE_KNOWN_SHAPE)) {
                return false;
            }
            BlockEvent.EntityPlaceEvent event = new BlockEvent.EntityPlaceEvent(snapshot,
                    level.getBlockState(pos.below()), breaker);
            NeoForge.EVENT_BUS.post(event);
            accepted = !event.isCanceled();
        } finally {
            if (!accepted) {
                level.setBlock(pos, previous, Block.UPDATE_KNOWN_SHAPE);
            }
        }
        if (accepted) {
            level.sendBlockUpdated(pos, previous, placed, Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, placed.getBlock());
        }
        return accepted;
    }

    public static boolean destroyByPlayer(ServerLevel level, BlockPos pos, BlockState state, Player breaker) {
        boolean removed = state.onDestroyedByPlayer(level, pos, breaker, false, level.getFluidState(pos));
        if (removed) {
            level.levelEvent(2001, pos, Block.getId(state));
        }
        return removed;
    }

    public static float explosionResistance(Level level, BlockPos pos, BlockState state, Explosion explosion) { return state.getExplosionResistance(level, pos, explosion); }
    public static Player fakePlayer(ServerLevel level, GameProfile profile) { return FakePlayerFactory.get(level, profile); }
}
