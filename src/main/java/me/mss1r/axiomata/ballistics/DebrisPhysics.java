package me.mss1r.axiomata.ballistics;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class DebrisPhysics {
    private final String debrisTag;
    private final String playerTag;
    private static final String TAG_CONTEXT = "axiomata:debris_context";
    private static final Map<String, DebrisPhysics> HANDLERS = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<ServerLevel, DebrisQuota> debrisQuotas = new WeakHashMap<>();
    /** Height above the struck face, in blocks, that ejecta start from. */
    private static final double MOUTH_CLEARANCE = 0.6D;
    /** Open space, in half-block steps, an ejected block needs in front of it to clear the crater. */
    private static final int EJECTA_CLEARANCE_STEPS = 5;

    private final String namespace;
    private final ProtectedBlockAccess blocks;
    private final java.util.function.BooleanSupplier enabled;
    private final java.util.function.IntSupplier maximum;

    public DebrisPhysics(String namespace, ProtectedBlockAccess blocks,
                         java.util.function.BooleanSupplier enabled, java.util.function.IntSupplier maximum) {
        this.namespace = java.util.Objects.requireNonNull(namespace);
        this.debrisTag = namespace + ":debris";
        this.playerTag = namespace + ":debris_player";
        this.blocks = java.util.Objects.requireNonNull(blocks);
        this.enabled = java.util.Objects.requireNonNull(enabled);
        this.maximum = java.util.Objects.requireNonNull(maximum);
    }

    /** Register during mod startup, before saved debris can tick. Repeating this call on the same instance is safe. */
    public void register() {
        DebrisPhysics previous = HANDLERS.putIfAbsent(namespace, this);
        if (previous != null && previous != this) {
            throw new IllegalArgumentException("Debris handler already registered: " + namespace);
        }
    }

    /** Null means an ordinary falling block, not artillery debris. */
    @Nullable
    public static Boolean handleLanding(ServerLevel level, FallingBlockEntity debris, BlockPos pos, BlockState state) {
        DebrisPhysics handler = handlerFor(debris.getPersistentData());
        return handler == null ? null : handler.placeDebris(level, debris, pos, state);
    }

    @Nullable
    static DebrisPhysics handlerFor(CompoundTag data) {
        DebrisPhysics handler = HANDLERS.get(data.getString(TAG_CONTEXT));
        if (handler == null) {
            for (DebrisPhysics candidate : HANDLERS.values()) {
                if (data.getBoolean(candidate.debrisTag)) {
                    handler = candidate;
                    break;
                }
            }
        }
        return handler;
    }

    public void scatterAffectedBlocks(ServerLevel level, Vec3 center, float radius, List<BlockPos> affectedBlocks,
                                              @Nullable Player breaker) {
        if (radius <= 0.0F || affectedBlocks.isEmpty() || !enabled.getAsBoolean()) {
            return;
        }

        for (Iterator<BlockPos> iterator = affectedBlocks.iterator(); iterator.hasNext(); ) {
            BlockPos pos = iterator.next();
            if (!canBecomeDebris(level, center, pos)) {
                continue;
            }
            if (!claimDebrisSlot(level)) {
                break;
            }

            BlockState state = level.getBlockState(pos);
            spawnDebris(level, radius, pos, state, Vec3.atCenterOf(pos), calculateMotion(level, center, pos, radius), breaker);
            iterator.remove();
        }
    }

    /**
     * Ejects a block broken out of a crater along {@code outward}, spread so it clears the rim. Blocks buried too deep
     * to get out are left as rubble for the caller to clear instead of being thrown back into the hole.
     */
    public boolean launchDestroyedBlock(ServerLevel level, BlockPos pos, Vec3 center, Vec3 outward,
                                               float blastPower, @Nullable Player breaker) {
        BlockState state = level.getBlockState(pos);
        if (!enabled.getAsBoolean() || !canBecomeDebris(level, pos, state)) {
            return false;
        }

        float power = Math.max(2.0F, blastPower);
        Vec3 motion = ejectaMotion(level, center, pos, outward, power);
        Vec3 from = craterMouth(level, center, pos, outward);
        if (!canEscape(level, pos, from, motion) || !claimDebrisSlot(level)) {
            return false;
        }
        spawnDebris(level, power, pos, state, from, motion, breaker);
        return true;
    }

    private boolean canBecomeDebris(ServerLevel level, Vec3 center, BlockPos pos) {
        if (pos.getY() < center.y - 0.35D) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        return canBecomeDebris(level, pos, state);
    }

    private boolean canBecomeDebris(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }

        if (state.getBlock() instanceof TntBlock || level.getBlockEntity(pos) != null) {
            return false;
        }

        return !state.getFluidState().isSource();
    }

    /**
     * Ejecta start point: just above the struck face, across from where the block broke, so ejecta leave over the rim.
     * If the mouth is blocked, it starts where the block was.
     */
    private static Vec3 craterMouth(ServerLevel level, Vec3 center, BlockPos pos, Vec3 outward) {
        if (outward.lengthSqr() < 1.0E-8D) {
            return Vec3.atCenterOf(pos);
        }
        Vec3 axis = outward.normalize();
        Vec3 offset = Vec3.atCenterOf(pos).subtract(center);
        Vec3 across = offset.subtract(axis.scale(offset.dot(axis)));
        if (across.lengthSqr() > 1.0D) {
            across = across.normalize();
        }
        Vec3 mouth = center.add(across).add(axis.scale(MOUTH_CLEARANCE));
        BlockPos at = BlockPos.containing(mouth);
        return !at.equals(pos) && !level.getBlockState(at).getCollisionShape(level, at).isEmpty()
                ? Vec3.atCenterOf(pos)
                : mouth;
    }

    private void spawnDebris(ServerLevel level, float blastPower, BlockPos pos, BlockState state,
                                    Vec3 from, Vec3 motion, @Nullable Player breaker) {
        // Fully set up before being added, so it first appears at its launch point.
        FallingBlockEntity debris = EntityType.FALLING_BLOCK.create(level);
        if (debris == null) {
            return;
        }
        CompoundTag saved = new CompoundTag();
        saved.put("BlockState", NbtUtils.writeBlockState(state.hasProperty(BlockStateProperties.WATERLOGGED)
                ? state.setValue(BlockStateProperties.WATERLOGGED, false)
                : state));
        debris.load(saved);
        debris.getPersistentData().putBoolean(debrisTag, true);
        debris.getPersistentData().putString(TAG_CONTEXT, namespace);
        if (breaker != null) {
            debris.getPersistentData().putUUID(playerTag, breaker.getUUID());
        }
        debris.setPos(from.x, from.y - 0.5D, from.z);
        debris.setOldPosAndRot();
        debris.setStartPos(pos);
        debris.dropItem = false;
        debris.setHurtsEntities(Math.max(1.0F, blastPower * 0.45F), Math.max(3, Mth.ceil(blastPower * 2.0F)));
        debris.setDeltaMovement(motion);
        level.setBlock(pos, state.getFluidState().createLegacyBlock(), 3);
        level.addFreshEntity(debris);
    }

    public boolean placeDebris(ServerLevel level, FallingBlockEntity debris, BlockPos pos, BlockState state) {
        var data = debris.getPersistentData();
        Player breaker = data.hasUUID(playerTag) ? blocks.attributedPlayer(level, data.getUUID(playerTag)) : null;
        boolean placed = blocks.placeBlock(level, pos, state, breaker);
        if (!placed) {
            debris.discard();
        }
        return placed;
    }

    /** Ejecta leave through the mouth in a cone around {@code outward}, so they land beyond the rim. */
    private static Vec3 ejectaMotion(ServerLevel level, Vec3 center, BlockPos pos, Vec3 outward, float power) {
        Vec3 axis = outward.lengthSqr() > 1.0E-8D ? outward.normalize() : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 offset = Vec3.atCenterOf(pos).subtract(center);
        Vec3 spread = offset.subtract(axis.scale(offset.dot(axis)));
        if (spread.lengthSqr() < 1.0E-4D) {
            spread = axis.cross(new Vec3(level.random.nextDouble() - 0.5D, level.random.nextDouble() - 0.5D,
                    level.random.nextDouble() - 0.5D));
        }
        if (spread.lengthSqr() < 1.0E-8D) {
            spread = axis.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        double speed = Mth.clamp(0.35D + power * 0.06D, 0.4D, 1.0D) * (0.85D + level.random.nextDouble() * 0.3D);
        Vec3 motion = axis.add(spread.normalize()).normalize().scale(speed);
        // Ejecta always go upward out of a crater, never down.
        return new Vec3(motion.x, Math.abs(motion.y) + 0.15D, motion.z);
    }

    private boolean canEscape(ServerLevel level, BlockPos pos, Vec3 from, Vec3 motion) {
        Vec3 step = motion.normalize().scale(0.5D);
        Vec3 point = from;
        for (int i = 0; i < EJECTA_CLEARANCE_STEPS; i++) {
            point = point.add(step);
            BlockPos at = BlockPos.containing(point);
            if (!at.equals(pos) && !level.getBlockState(at).getCollisionShape(level, at).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static Vec3 calculateMotion(ServerLevel level, Vec3 center, BlockPos pos, float radius) {
        Vec3 offset = Vec3.atCenterOf(pos).subtract(center);
        Vec3 direction = offset.lengthSqr() < 1.0E-5D
                ? new Vec3(level.random.nextDouble() - 0.5D, 0.35D, level.random.nextDouble() - 0.5D)
                : offset;
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 1.0E-5D) {
            horizontal = new Vec3(level.random.nextDouble() - 0.5D, 0.0D, level.random.nextDouble() - 0.5D);
        }

        double distance = Math.max(0.25D, offset.length());
        double falloff = Mth.clamp(1.0D - distance / Math.max(1.0D, radius + 2.0D), 0.35D, 1.0D);
        double outward = Mth.clamp(0.85D + radius * 0.18D, 0.85D, 2.25D) * falloff;
        double upward = Mth.clamp(0.55D + radius * 0.1D + level.random.nextDouble() * 0.35D,
                0.55D, 1.65D) * Math.sqrt(falloff);

        return horizontal.normalize().scale(outward).add(0.0D, upward, 0.0D);
    }

    private boolean claimDebrisSlot(ServerLevel level) {
        long gameTime = level.getGameTime();
        DebrisQuota quota = debrisQuotas.computeIfAbsent(level, ignored -> new DebrisQuota(gameTime));
        if (quota.gameTime != gameTime) {
            quota.gameTime = gameTime;
            quota.used = 0;
        }

        if (quota.used >= maximum.getAsInt()) {
            return false;
        }
        quota.used++;
        return true;
    }

    private static final class DebrisQuota {
        private long gameTime;
        private int used;

        private DebrisQuota(long gameTime) {
            this.gameTime = gameTime;
        }
    }
}
