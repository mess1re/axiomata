package me.mss1r.axiomata.platform.fabric;

import com.mojang.authlib.GameProfile;
import eu.pb4.common.protection.api.CommonProtection;
import me.mss1r.axiomata.fabric.PersistentDataHolder;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class AxiomataPlatformImpl {
    private AxiomataPlatformImpl() {}
    public static CompoundTag persistentData(Entity entity) { return ((PersistentDataHolder) entity).axiomata$persistentData(); }
    public static boolean canReach(ServerPlayer player, Entity target, double padding) {
        return target.getBoundingBox().distanceToSqr(player.getEyePosition()) < Math.pow(entityReach(player) + padding, 2);
    }
    public static double entityReach(Player player) {
        //? if >=1.21 {
        return player.entityInteractionRange();
        //?} else {
        /*return player.isCreative() ? 6.0D : 3.0D;
        *///?}
    }
    public static double blockReach(Player player) {
        //? if >=1.21 {
        return player.blockInteractionRange();
        //?} else {
        /*return player.isCreative() ? 5.0D : 4.5D;
        *///?}
    }
    public static Entity logicalTarget(Entity target) { return target instanceof EnderDragonPart part ? part.parentMob : target; }
    public static boolean blocksWithShield(ItemStack stack) { return stack.getUseAnimation() == UseAnim.BLOCK; }
    public static boolean breakCancelled(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        return !CommonProtection.canBreakBlock(level, pos, player.getGameProfile(), player)
                || !PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, level.getBlockEntity(pos));
    }
    public static boolean placeProtectedBlock(ServerLevel level, BlockPos pos, BlockState placed, Player player) {
        // Query the actual destination without simulating a right-click or mutating the world first.
        if (player == null || !CommonProtection.canPlaceBlock(level, pos, player.getGameProfile(), player)) return false;
        return level.setBlockAndUpdate(pos, placed);
    }
    public static boolean destroyByPlayer(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        var blockEntity = level.getBlockEntity(pos);
        state.getBlock().playerWillDestroy(level, pos, state, player);
        boolean removed = level.removeBlock(pos, false);
        if (removed) {
            level.levelEvent(2001, pos, Block.getId(state));
            PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level, player, pos, state, blockEntity);
        }
        return removed;
    }
    public static float explosionResistance(Level level, BlockPos pos, BlockState state, Explosion explosion) {
        return state.getBlock().getExplosionResistance();
    }
    public static Player fakePlayer(ServerLevel level, GameProfile profile) { return FakePlayer.get(level, profile); }
}
