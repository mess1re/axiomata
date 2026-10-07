package me.mss1r.axiomata.ballistics;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
//? if forge {
/*import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.BlockEvent;
*///?} else {
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
//?}

import java.util.UUID;

public final class ProtectedBlockAccess {
    private static final int BLOCK_BREAK_EFFECT = 2001;
    private static final String OFFLINE_NAME = "[Artillery]";

    public enum Policy { EVERYWHERE, NEVER, RESPECT_PROTECTION }
    private final java.util.function.Supplier<Policy> policy;
    private final String offlineName;

    public ProtectedBlockAccess(java.util.function.Supplier<Policy> policy) {
        this(policy, OFFLINE_NAME);
    }

    public ProtectedBlockAccess(java.util.function.Supplier<Policy> policy, String offlineName) {
        this.policy = java.util.Objects.requireNonNull(policy);
        this.offlineName = java.util.Objects.requireNonNull(offlineName);
    }

    /**
     * Whether an impact attributed to this player may damage a block under the server's terrain rule. With
     * {@code RESPECT_PROTECTION} it runs the same checks as the player breaking the block by hand, so claim mods and
     * spawn protection apply.
     */
    public boolean mayDamage(ServerLevel level, BlockPos pos, BlockState state, @Nullable Player breaker) {
        return switch (policy.get()) {
            case EVERYWHERE -> true;
            case NEVER -> false;
            case RESPECT_PROTECTION -> breaker != null
                    && level.mayInteract(breaker, pos)
                    && !breakCancelled(level, pos, state, breaker);
        };
    }

    private static boolean breakCancelled(ServerLevel level, BlockPos pos, BlockState state, Player breaker) {
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, breaker);
        //? if forge {
        /*MinecraftForge.EVENT_BUS.post(event);
        *///?} else {
        NeoForge.EVENT_BUS.post(event);
        //?}
        return event.isCanceled();
    }

    /** Fire is a placement, so it also has to pass the claim mod's placement event. */
    public boolean placeFire(ServerLevel level, BlockPos pos, @Nullable Player breaker) {
        BlockState previous = level.getBlockState(pos);
        BlockState fire = Blocks.FIRE.defaultBlockState();
        return previous.isAir() && fire.canSurvive(level, pos) && placeBlock(level, pos, fire, breaker);
    }

    public boolean placeBlock(ServerLevel level, BlockPos pos, BlockState placed, @Nullable Player breaker) {
        BlockState previous = level.getBlockState(pos);
        if (!mayDamage(level, pos, previous, breaker)
                || !mayDamage(level, pos.below(), level.getBlockState(pos.below()), breaker)) {
            return false;
        }
        if (policy.get() == Policy.EVERYWHERE) {
            return level.setBlockAndUpdate(pos, placed);
        }
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        boolean accepted = false;
        try {
            // Hold back neighbours and client updates until the placement is accepted.
            if (!level.setBlock(pos, placed, Block.UPDATE_KNOWN_SHAPE)) {
                return false;
            }
            BlockEvent.EntityPlaceEvent event = new BlockEvent.EntityPlaceEvent(snapshot,
                    level.getBlockState(pos.below()), breaker);
            //? if forge {
            /*MinecraftForge.EVENT_BUS.post(event);
            *///?} else {
            NeoForge.EVENT_BUS.post(event);
            //?}
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

    public boolean breakBlock(ServerLevel level, BlockPos pos, @Nullable Player breaker) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !mayDamage(level, pos, state, breaker)) {
            return false;
        }
        if (breaker == null) {
            return level.destroyBlock(pos, false);
        }

        boolean removed = state.onDestroyedByPlayer(level, pos, breaker, false, level.getFluidState(pos));
        //? if neoforge {
        if (removed) {
            level.levelEvent(BLOCK_BREAK_EFFECT, pos, Block.getId(state));
        }
        //?}
        return removed;
    }

    public static float damageResistance(Level level, BlockPos pos, BlockState state, Explosion probe) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F) {
            return hardness;
        }
        return Math.min(hardness, Math.max(0.0F, state.getExplosionResistance(level, pos, probe)));
    }

    public static Explosion damageProbe(Level level, Vec3 center, @Nullable Entity source) {
        return new Explosion(level, source, center.x, center.y, center.z, 0.0F, false,
                Explosion.BlockInteraction.KEEP);
    }

    @Nullable
    public static Player playerFor(ServerLevel level, @Nullable UUID playerId) {
        return playerFor(level, playerId, OFFLINE_NAME);
    }

    @Nullable
    public Player attributedPlayer(ServerLevel level, @Nullable UUID playerId) {
        return playerFor(level, playerId, offlineName);
    }

    @Nullable
    private static Player playerFor(ServerLevel level, @Nullable UUID playerId, String offlineName) {
        if (playerId == null) {
            return null;
        }
        MinecraftServer server = level.getServer();
        Player online = server.getPlayerList().getPlayer(playerId);
        if (online != null) {
            return online;
        }
        GameProfileCache cache = server.getProfileCache();
        GameProfile profile = cache == null ? null : cache.get(playerId).orElse(null);
        return FakePlayerFactory.get(level, profile != null ? profile : new GameProfile(playerId, offlineName));
    }
}
