package me.mss1r.axiomata.blueprint.network;

import dev.architectury.networking.NetworkManager;
import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.blueprint.tracing.OutlineCatalog;
import net.minecraft.network.FriendlyByteBuf;
//? if neoforge {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import me.mss1r.axiomata.blueprint.BlueprintModule;
//?}

import java.util.ArrayList;
import java.util.List;

//? if forge {
/*public record S2COutlineIndexPacket(List<String> blueprintIds) {
*///?} else {
public record S2COutlineIndexPacket(List<String> blueprintIds) implements CustomPacketPayload {
    public static final Type<S2COutlineIndexPacket> TYPE = new Type<>(
            ResourceIds.id(BlueprintModule.MOD_ID, "outline_index"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2COutlineIndexPacket> STREAM_CODEC =
            StreamCodec.ofMember(S2COutlineIndexPacket::write, S2COutlineIndexPacket::decode);

    private void write(RegistryFriendlyByteBuf buffer) {
        encode(this, buffer);
    }

    @Override
    public Type<S2COutlineIndexPacket> type() {
        return TYPE;
    }
    //?}
    private static final int MAX_ID_LENGTH = 256;
    private static final int MAX_ENTRIES = 1024;

    public static void encode(S2COutlineIndexPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(Math.min(packet.blueprintIds.size(), MAX_ENTRIES));
        packet.blueprintIds.stream().limit(MAX_ENTRIES).forEach(id -> buffer.writeUtf(id, MAX_ID_LENGTH));
    }

    public static S2COutlineIndexPacket decode(FriendlyByteBuf buffer) {
        int count = Math.min(buffer.readVarInt(), MAX_ENTRIES);
        List<String> ids = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            ids.add(buffer.readUtf(MAX_ID_LENGTH));
        }
        return new S2COutlineIndexPacket(ids);
    }

    public static void handle(S2COutlineIndexPacket packet, NetworkManager.PacketContext context) {
        context.queue(() -> OutlineCatalog.setDrawable(packet.blueprintIds()));
    }
}
