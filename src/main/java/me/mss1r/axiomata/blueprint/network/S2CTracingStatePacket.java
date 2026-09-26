package me.mss1r.axiomata.blueprint.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import me.mss1r.axiomata.blueprint.blockentity.DrawingTableBlockEntity;
import me.mss1r.axiomata.blueprint.tracing.BlueprintOutline;
import me.mss1r.axiomata.blueprint.tracing.TracingSession;
import me.mss1r.axiomata.blueprint.client.ClientTracing;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//? if neoforge {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import me.mss1r.axiomata.blueprint.BlueprintModule;
//?}

// Data-pack outlines are server-side, so the chosen mask and the saved ink have to travel with
// the table state. This is sent when a drawing is selected or an unfinished table is reopened.
//? if forge {
/*public record S2CTracingStatePacket(String blueprintId, ResourceLocation texture, int resolution,
                                    byte[] mask, byte[] covered, int wandered) {
*///?} else {
public record S2CTracingStatePacket(String blueprintId, ResourceLocation texture, int resolution,
                                    byte[] mask, byte[] covered, int wandered) implements CustomPacketPayload {
    public static final Type<S2CTracingStatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BlueprintModule.MOD_ID, "tracing_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CTracingStatePacket> STREAM_CODEC =
            StreamCodec.ofMember(S2CTracingStatePacket::write, S2CTracingStatePacket::decode);

    private void write(RegistryFriendlyByteBuf buffer) {
        encode(this, buffer);
    }

    @Override
    public Type<S2CTracingStatePacket> type() {
        return TYPE;
    }
    //?}
    private static final int MAX_ID_LENGTH = 256;
    private static final int MAX_MASK_BYTES = 8192;

    public static S2CTracingStatePacket of(DrawingTableBlockEntity table, BlueprintOutline outline) {
        TracingSession session = table.session();
        return new S2CTracingStatePacket(table.blueprintId(), outline.texture(),
                outline.mask().resolution(), outline.mask().bits(),
                session == null ? new byte[0] : session.snapshot(),
                session == null ? 0 : session.wandered());
    }

    public static void encode(S2CTracingStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.blueprintId, MAX_ID_LENGTH);
        buffer.writeResourceLocation(packet.texture);
        buffer.writeVarInt(packet.resolution);
        buffer.writeByteArray(packet.mask);
        buffer.writeByteArray(packet.covered);
        buffer.writeVarInt(packet.wandered);
    }

    public static S2CTracingStatePacket decode(FriendlyByteBuf buffer) {
        return new S2CTracingStatePacket(buffer.readUtf(MAX_ID_LENGTH), buffer.readResourceLocation(),
                buffer.readVarInt(), buffer.readByteArray(MAX_MASK_BYTES),
                buffer.readByteArray(MAX_MASK_BYTES), buffer.readVarInt());
    }

    public static void handle(S2CTracingStatePacket packet, NetworkManager.PacketContext context) {
        EnvExecutor.runInEnv(Env.CLIENT,
                () -> () -> context.queue(() -> ClientTracing.accept(packet)));
    }
}
