package me.mss1r.axiomata.blueprint.network;

import dev.architectury.networking.NetworkManager;
import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.blueprint.internal.definition.BlueprintDefinitionCatalog;
import net.minecraft.network.FriendlyByteBuf;
//? if neoforge {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import me.mss1r.axiomata.blueprint.BlueprintModule;
//?}

//? if forge {
/*public record S2CBlueprintCatalogPacket(String catalogJson) {
*///?} else {
public record S2CBlueprintCatalogPacket(String catalogJson) implements CustomPacketPayload {
    public static final Type<S2CBlueprintCatalogPacket> TYPE = new Type<>(
            ResourceIds.id(BlueprintModule.MOD_ID, "blueprint_catalog"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CBlueprintCatalogPacket> STREAM_CODEC =
            StreamCodec.ofMember(S2CBlueprintCatalogPacket::write, S2CBlueprintCatalogPacket::decode);

    private void write(RegistryFriendlyByteBuf buffer) {
        encode(this, buffer);
    }

    @Override
    public Type<S2CBlueprintCatalogPacket> type() {
        return TYPE;
    }
    //?}
    private static final int MAX_CATALOG_LENGTH = 1_048_576;

    public static void encode(S2CBlueprintCatalogPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.catalogJson, MAX_CATALOG_LENGTH);
    }

    public static S2CBlueprintCatalogPacket decode(FriendlyByteBuf buffer) {
        return new S2CBlueprintCatalogPacket(buffer.readUtf(MAX_CATALOG_LENGTH));
    }

    public static void handle(S2CBlueprintCatalogPacket packet, NetworkManager.PacketContext context) {
        context.queue(() -> BlueprintDefinitionCatalog.applySyncedCatalog(packet.catalogJson));
    }
}
