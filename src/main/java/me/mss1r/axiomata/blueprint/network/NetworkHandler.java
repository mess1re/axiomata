package me.mss1r.axiomata.blueprint.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import io.netty.buffer.Unpooled;
import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.blueprint.BlueprintModule;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
//? if neoforge {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class NetworkHandler {
    public static final Sender INSTANCE = NetworkHandler::sendToServer;

    //? if forge {
    /*private static final ResourceLocation BLUEPRINT_CATALOG = id("blueprint_catalog");
    private static final ResourceLocation USE_BLUEPRINT = id("use_blueprint");
    private static final ResourceLocation SET_TABLE_SLOT = id("set_table_slot");
    private static final ResourceLocation SELECT_BLUEPRINT = id("select_blueprint");
    private static final ResourceLocation STROKE = id("stroke");
    private static final ResourceLocation TRACING_STATE = id("tracing_state");
    private static final ResourceLocation OUTLINE_INDEX = id("outline_index");
    private static final ResourceLocation TAKE_TABLE_RESULT = id("take_table_result");
    *///?}

    private NetworkHandler() {
    }

    public static void register() {
        //? if forge {
        /*register(NetworkManager.c2s(), USE_BLUEPRINT,
                C2SUseBlueprintPacket::decode, C2SUseBlueprintPacket::handle);
        register(NetworkManager.c2s(), SET_TABLE_SLOT,
                C2SSetTableSlotPacket::decode, C2SSetTableSlotPacket::handle);
        register(NetworkManager.c2s(), SELECT_BLUEPRINT,
                C2SSelectBlueprintPacket::decode, C2SSelectBlueprintPacket::handle);
        register(NetworkManager.c2s(), STROKE,
                C2SStrokePacket::decode, C2SStrokePacket::handle);
        register(NetworkManager.c2s(), TAKE_TABLE_RESULT,
                C2STakeResultFromTablePacket::decode, C2STakeResultFromTablePacket::handle);
        *///?} else {
        NetworkManager.registerReceiver(NetworkManager.c2s(), C2SUseBlueprintPacket.TYPE,
                C2SUseBlueprintPacket.STREAM_CODEC, C2SUseBlueprintPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.c2s(), C2SSetTableSlotPacket.TYPE,
                C2SSetTableSlotPacket.STREAM_CODEC, C2SSetTableSlotPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.c2s(), C2SSelectBlueprintPacket.TYPE,
                C2SSelectBlueprintPacket.STREAM_CODEC, C2SSelectBlueprintPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.c2s(), C2SStrokePacket.TYPE,
                C2SStrokePacket.STREAM_CODEC, C2SStrokePacket::handle);
        NetworkManager.registerReceiver(NetworkManager.c2s(), C2STakeResultFromTablePacket.TYPE,
                C2STakeResultFromTablePacket.STREAM_CODEC, C2STakeResultFromTablePacket::handle);
        EnvExecutor.runInEnv(Env.SERVER, () -> () -> {
            NetworkManager.registerS2CPayloadType(S2CBlueprintCatalogPacket.TYPE,
                    S2CBlueprintCatalogPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(S2CTracingStatePacket.TYPE,
                    S2CTracingStatePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(S2COutlineIndexPacket.TYPE,
                    S2COutlineIndexPacket.STREAM_CODEC);
        });
        //?}
        EnvExecutor.runInEnv(Env.CLIENT, () -> NetworkHandler::registerClientReceivers);
    }

    private static void registerClientReceivers() {
        //? if forge {
        /*register(NetworkManager.s2c(), BLUEPRINT_CATALOG,
                S2CBlueprintCatalogPacket::decode, S2CBlueprintCatalogPacket::handle);
        register(NetworkManager.s2c(), TRACING_STATE,
                S2CTracingStatePacket::decode, S2CTracingStatePacket::handle);
        register(NetworkManager.s2c(), OUTLINE_INDEX,
                S2COutlineIndexPacket::decode, S2COutlineIndexPacket::handle);
        *///?} else {
        NetworkManager.registerReceiver(NetworkManager.s2c(), S2CBlueprintCatalogPacket.TYPE,
                S2CBlueprintCatalogPacket.STREAM_CODEC, S2CBlueprintCatalogPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.s2c(), S2CTracingStatePacket.TYPE,
                S2CTracingStatePacket.STREAM_CODEC, S2CTracingStatePacket::handle);
        NetworkManager.registerReceiver(NetworkManager.s2c(), S2COutlineIndexPacket.TYPE,
                S2COutlineIndexPacket.STREAM_CODEC, S2COutlineIndexPacket::handle);
        //?}
    }

    //? if forge {
    /*private static <T> void register(NetworkManager.Side side, ResourceLocation id,
                                     Function<FriendlyByteBuf, T> decoder,
                                     BiConsumer<T, NetworkManager.PacketContext> handler) {
        NetworkManager.registerReceiver(side, id, (buffer, context) -> handler.accept(decoder.apply(buffer), context));
    }
    *///?}

    public static void sendToServer(Object packet) {
        //? if forge {
        /*FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        ResourceLocation id;
        if (packet instanceof C2SUseBlueprintPacket value) {
            C2SUseBlueprintPacket.encode(value, buffer);
            id = USE_BLUEPRINT;
        } else if (packet instanceof C2SSetTableSlotPacket value) {
            C2SSetTableSlotPacket.encode(value, buffer);
            id = SET_TABLE_SLOT;
        } else if (packet instanceof C2SSelectBlueprintPacket value) {
            C2SSelectBlueprintPacket.encode(value, buffer);
            id = SELECT_BLUEPRINT;
        } else if (packet instanceof C2SStrokePacket value) {
            C2SStrokePacket.encode(value, buffer);
            id = STROKE;
        } else if (packet instanceof C2STakeResultFromTablePacket value) {
            C2STakeResultFromTablePacket.encode(value, buffer);
            id = TAKE_TABLE_RESULT;
        } else {
            throw new IllegalArgumentException("Unsupported client packet: " + packet.getClass().getName());
        }
        NetworkManager.sendToServer(id, buffer);
        *///?} else {
        NetworkManager.sendToServer((CustomPacketPayload) packet);
        //?}
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        //? if forge {
        /*FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        ResourceLocation id;
        if (packet instanceof S2CBlueprintCatalogPacket value) {
            S2CBlueprintCatalogPacket.encode(value, buffer);
            id = BLUEPRINT_CATALOG;
        } else if (packet instanceof S2CTracingStatePacket value) {
            S2CTracingStatePacket.encode(value, buffer);
            id = TRACING_STATE;
        } else if (packet instanceof S2COutlineIndexPacket value) {
            S2COutlineIndexPacket.encode(value, buffer);
            id = OUTLINE_INDEX;
        } else {
            throw new IllegalArgumentException("Unsupported server packet: " + packet.getClass().getName());
        }
        NetworkManager.sendToPlayer(player, id, buffer);
        *///?} else {
        NetworkManager.sendToPlayer(player, (CustomPacketPayload) packet);
        //?}
    }

    private static ResourceLocation id(String path) {
        return ResourceIds.id(BlueprintModule.MOD_ID, path);
    }

    @FunctionalInterface
    public interface Sender {
        void sendToServer(Object packet);
    }
}
