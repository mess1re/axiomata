package me.mss1r.axiomata.data.profile;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/** NBT map used by profile packets; each mod keeps its own packet ID and catalog. */
public final class ProfileSnapshotCodec<T> {
    private final Codec<Map<ResourceLocation, T>> codec;

    public ProfileSnapshotCodec(Codec<T> profileCodec) {
        codec = Codec.unboundedMap(ResourceLocation.CODEC, profileCodec);
    }

    public void write(FriendlyByteBuf buffer, Map<ResourceLocation, T> profiles) {
        buffer.writeNbt((CompoundTag) codec.encodeStart(NbtOps.INSTANCE, profiles).result()
                .orElseThrow(() -> new IllegalStateException("Profile snapshot could not be encoded")));
    }

    public Map<ResourceLocation, T> read(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return tag == null ? Map.of() : codec.parse(NbtOps.INSTANCE, tag).result().orElse(Map.of());
    }
}
