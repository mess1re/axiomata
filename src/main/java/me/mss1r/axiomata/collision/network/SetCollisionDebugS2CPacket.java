package me.mss1r.axiomata.collision.network;

import dev.architectury.networking.NetworkManager;
import me.mss1r.axiomata.Axiomata;
import net.minecraft.network.FriendlyByteBuf;
//? if neoforge {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
//?}

//? if forge {
/*public record SetCollisionDebugS2CPacket(boolean enabled) {
*///?} else {
public record SetCollisionDebugS2CPacket(boolean enabled) implements CustomPacketPayload {
    public static final Type<SetCollisionDebugS2CPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Axiomata.MOD_ID, "set_collision_debug"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetCollisionDebugS2CPacket> STREAM_CODEC =
            StreamCodec.ofMember(SetCollisionDebugS2CPacket::write, SetCollisionDebugS2CPacket::decode);

    private void write(RegistryFriendlyByteBuf buffer) {
        encode(this, buffer);
    }

    @Override
    public Type<SetCollisionDebugS2CPacket> type() {
        return TYPE;
    }
    //?}

    public static void encode(SetCollisionDebugS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.enabled);
    }

    public static SetCollisionDebugS2CPacket decode(FriendlyByteBuf buffer) {
        return new SetCollisionDebugS2CPacket(buffer.readBoolean());
    }

    public static void handle(SetCollisionDebugS2CPacket packet, NetworkManager.PacketContext context) {
        context.queue(() -> ClientHandler.setEnabled(packet.enabled));
    }

    private static final class ClientHandler {
        private static void setEnabled(boolean enabled) {
            me.mss1r.axiomata.collision.client.StructureCollisionDebugRenderer.setEnabled(enabled);
        }
    }
}
