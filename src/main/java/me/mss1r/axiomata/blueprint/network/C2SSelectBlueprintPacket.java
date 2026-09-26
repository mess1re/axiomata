package me.mss1r.axiomata.blueprint.network;

import dev.architectury.networking.NetworkManager;
import me.mss1r.axiomata.blueprint.blockentity.DrawingTableBlockEntity;
import me.mss1r.axiomata.blueprint.tracing.BlueprintOutline;
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
/*public record C2SSelectBlueprintPacket(String blueprintId) {
*///?} else {
public record C2SSelectBlueprintPacket(String blueprintId) implements CustomPacketPayload {
    public static final Type<C2SSelectBlueprintPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BlueprintModule.MOD_ID, "select_blueprint"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSelectBlueprintPacket> STREAM_CODEC =
            StreamCodec.ofMember(C2SSelectBlueprintPacket::write, C2SSelectBlueprintPacket::decode);

    private void write(RegistryFriendlyByteBuf buffer) {
        encode(this, buffer);
    }

    @Override
    public Type<C2SSelectBlueprintPacket> type() {
        return TYPE;
    }
    //?}
    private static final int MAX_ID_LENGTH = 256;

    public static void encode(C2SSelectBlueprintPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.blueprintId, MAX_ID_LENGTH);
    }

    public static C2SSelectBlueprintPacket decode(FriendlyByteBuf buffer) {
        return new C2SSelectBlueprintPacket(buffer.readUtf(MAX_ID_LENGTH));
    }

    public static void handle(C2SSelectBlueprintPacket packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof DrawingTableMenu menu)) {
                return;
            }
            DrawingTableBlockEntity table = menu.table();
            if (table == null || !menu.stillValid(player) || !table.select(packet.blueprintId())) {
                return;
            }
            menu.refreshResult();
            menu.broadcastChanges();

            BlueprintOutline outline = table.outline();
            if (outline != null) {
                NetworkHandler.sendToPlayer(player, S2CTracingStatePacket.of(table, outline));
            }
        });
    }
}
