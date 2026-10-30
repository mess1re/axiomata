package me.mss1r.axiomata.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Hooks for loader extensions; common code owns the gameplay policy. */
public final class AxiomataPlatform {
    private AxiomataPlatform() {}
    @ExpectPlatform public static net.minecraft.nbt.CompoundTag persistentData(Entity entity) { throw new AssertionError(); }
    @ExpectPlatform public static boolean canReach(net.minecraft.server.level.ServerPlayer player, Entity entity, double padding) { throw new AssertionError(); }
    @ExpectPlatform public static double entityReach(Player player) { throw new AssertionError(); }
    @ExpectPlatform public static double blockReach(Player player) { throw new AssertionError(); }
    @ExpectPlatform public static Entity logicalTarget(@Nullable Entity entity) { throw new AssertionError(); }
    @ExpectPlatform public static boolean blocksWithShield(ItemStack stack) { throw new AssertionError(); }
    @ExpectPlatform public static boolean breakCancelled(ServerLevel level, BlockPos pos, BlockState state, Player player) { throw new AssertionError(); }
    @ExpectPlatform public static boolean placeProtectedBlock(ServerLevel level, BlockPos pos, BlockState placed, Player player) { throw new AssertionError(); }
    @ExpectPlatform public static boolean destroyByPlayer(ServerLevel level, BlockPos pos, BlockState state, Player player) { throw new AssertionError(); }
    @ExpectPlatform public static float explosionResistance(Level level, BlockPos pos, BlockState state, Explosion explosion) { throw new AssertionError(); }
    @ExpectPlatform public static Player fakePlayer(ServerLevel level, GameProfile profile) { throw new AssertionError(); }
}
