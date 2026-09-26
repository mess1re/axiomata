package me.mss1r.axiomata.blueprint.network;

import dev.architectury.networking.NetworkManager;
import me.mss1r.axiomata.blueprint.blockentity.DrawingTableBlockEntity;
import me.mss1r.axiomata.blueprint.tracing.TracingRules;
import me.mss1r.axiomata.blueprint.menu.DrawingTableMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
//? if neoforge {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import me.mss1r.axiomata.blueprint.BlueprintModule;
//?}

//? if forge {
/*public record C2SStrokePacket(boolean lift, byte[] points) {
*///?} else {
public record C2SStrokePacket(boolean lift, byte[] points) implements CustomPacketPayload {
    public static final Type<C2SStrokePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BlueprintModule.MOD_ID, "stroke"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SStrokePacket> STREAM_CODEC =
            StreamCodec.ofMember(C2SStrokePacket::write, C2SStrokePacket::decode);

    private void write(RegistryFriendlyByteBuf buffer) {
        encode(this, buffer);
    }

    @Override
    public Type<C2SStrokePacket> type() {
        return TYPE;
    }
    //?}
    public static void encode(C2SStrokePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.lift);
        buffer.writeByteArray(packet.points);
    }

    public static C2SStrokePacket decode(FriendlyByteBuf buffer) {
        boolean lift = buffer.readBoolean();
        return new C2SStrokePacket(lift, buffer.readByteArray(TracingRules.MAX_SAMPLES_PER_TICK * 2));
    }

    public static void handle(C2SStrokePacket packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof DrawingTableMenu menu)) {
                return;
            }
            DrawingTableBlockEntity table = menu.table();
            if (table == null || !menu.stillValid(player)) {
                return;
            }

            int samples = Math.min(packet.points().length / 2, TracingRules.MAX_SAMPLES_PER_TICK);
            if (samples > 0 && menu.allowStroke(player.level().getGameTime(), samples)) {
                int[] scaled = new int[samples * 2];
                for (int index = 0; index < samples * 2; index++) {
                    scaled[index] = packet.points()[index] & 0xFF;
                }
                table.trace(scaled, samples);
                menu.refreshResult();
                menu.broadcastChanges();
            }
            if (packet.lift()) {
                table.liftPen();
            }
        });
    }
}
