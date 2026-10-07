package me.mss1r.axiomata.ballistics.damage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
//? if neoforge {
import net.minecraft.core.HolderLookup;
import net.minecraft.util.datafix.DataFixTypes;
//?}

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Cracks left in blocks by hits that didn't break them. Cracks don't heal; they are saved per dimension until the block
 * breaks or changes.
 */
public final class StructuralDamageSystem {
    /** Resend interval for cracks; clients drop a crack after 20 seconds without an update. */
    private static final int OVERLAY_REFRESH_TICKS = 200;

    private StructuralDamageSystem() {
    }

    /** Crack progress of {@code state} at {@code pos}, from 0 to 1. */
    public static float progress(ServerLevel level, BlockPos pos, BlockState state) {
        Crack crack = Cracks.of(level).cracks.get(pos);
        return crack != null && crack.matches(state) ? crack.progress() : 0.0F;
    }

    public static ImpactResult applyImpact(ServerLevel level, BlockPos pos,
                                           float impact, float hardness) {
        BlockState blockState = level.getBlockState(pos);
        if (impact <= 0.0F || !Float.isFinite(impact) || hardness < 0.0F || blockState.isAir()) {
            return ImpactResult.IGNORED;
        }

        Cracks cracks = Cracks.of(level);
        BlockPos key = pos.immutable();
        Crack previous = cracks.cracks.get(key);
        float previousProgress = previous != null && previous.matches(blockState)
                ? previous.progress()
                : 0.0F;
        float progress = hardness == 0.0F
                ? 1.0F
                : previousProgress + impact / hardness;
        cracks.setDirty();

        if (progress >= 1.0F) {
            cracks.cracks.remove(key);
            BlockCrackOverlay.clear(level, key);
            return ImpactResult.BREAK_BLOCK;
        }

        cracks.cracks.put(key, new Crack(blockState, progress));
        BlockCrackOverlay.show(level, key, progress);
        return ImpactResult.ACCUMULATED;
    }

    public static void tick(ServerLevel level) {
        Cracks cracks = Cracks.of(level);
        boolean refresh = level.getGameTime() % OVERLAY_REFRESH_TICKS == 0L;
        Iterator<Map.Entry<BlockPos, Crack>> iterator = cracks.cracks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Crack> tracked = iterator.next();
            BlockPos pos = tracked.getKey();
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (!tracked.getValue().matches(level.getBlockState(pos))) {
                BlockCrackOverlay.clear(level, pos);
                iterator.remove();
                cracks.setDirty();
            } else if (refresh) {
                BlockCrackOverlay.show(level, pos, tracked.getValue().progress());
            }
        }
    }

    public enum ImpactResult {
        IGNORED,
        ACCUMULATED,
        BREAK_BLOCK
    }

    private record Crack(BlockState blockState, float progress) {
        boolean matches(BlockState current) {
            return !current.isAir() && blockState.equals(current);
        }
    }

    /** Cracks of one dimension, stored in its saved data. */
    private static final class Cracks extends SavedData {
        // Existing worlds already use this key.
        private static final String DATA_NAME = "siegeworks_cracks";
        private static final String TAG_CRACKS = "Cracks";
        private static final String TAG_POS = "Pos";
        private static final String TAG_STATE = "State";
        private static final String TAG_PROGRESS = "Progress";

        private final Map<BlockPos, Crack> cracks = new HashMap<>();

        static Cracks of(ServerLevel level) {
            //? if forge {
            /*return level.getDataStorage().computeIfAbsent(Cracks::load, Cracks::new, DATA_NAME);
            *///?} else {
            return level.getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(Cracks::new, Cracks::load, DataFixTypes.SAVED_DATA_COMMAND_STORAGE),
                    DATA_NAME);
            //?}
        }

        //? if forge {
        /*private static Cracks load(CompoundTag tag) {
        *///?} else {
        private static Cracks load(CompoundTag tag, HolderLookup.Provider registries) {
        //?}
            Cracks data = new Cracks();
            for (Tag value : tag.getList(TAG_CRACKS, Tag.TAG_COMPOUND)) {
                CompoundTag entry = (CompoundTag) value;
                BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),
                        entry.getCompound(TAG_STATE));
                if (!state.isAir()) {
                    data.cracks.put(BlockPos.of(entry.getLong(TAG_POS)),
                            new Crack(state, entry.getFloat(TAG_PROGRESS)));
                }
            }
            return data;
        }

        @Override
        //? if forge {
        /*public CompoundTag save(CompoundTag tag) {
        *///?} else {
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        //?}
            ListTag entries = new ListTag();
            cracks.forEach((pos, crack) -> {
                CompoundTag entry = new CompoundTag();
                entry.putLong(TAG_POS, pos.asLong());
                entry.put(TAG_STATE, NbtUtils.writeBlockState(crack.blockState()));
                entry.putFloat(TAG_PROGRESS, crack.progress());
                entries.add(entry);
            });
            tag.put(TAG_CRACKS, entries);
            return tag;
        }
    }
}
