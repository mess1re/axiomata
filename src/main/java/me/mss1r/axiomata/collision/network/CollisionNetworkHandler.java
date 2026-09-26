package me.mss1r.axiomata.collision.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import io.netty.buffer.Unpooled;
import me.mss1r.axiomata.Axiomata;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
//? if neoforge {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

public final class CollisionNetworkHandler {
    //? if forge {
    /*private static final ResourceLocation SET_COLLISION_DEBUG = id("set_collision_debug");
    *///?}

    private CollisionNetworkHandler() {
    }

    public static void register() {
        //? if neoforge {
        EnvExecutor.runInEnv(Env.SERVER, () -> () ->
                NetworkManager.registerS2CPayloadType(
                        SetCollisionDebugS2CPacket.TYPE,
                        SetCollisionDebugS2CPacket.STREAM_CODEC));
        //?}
        EnvExecutor.runInEnv(Env.CLIENT, () -> CollisionNetworkHandler::registerClientReceiver);
    }

    private static void registerClientReceiver() {
        //? if forge {
        /*NetworkManager.registerReceiver(
                NetworkManager.s2c(), SET_COLLISION_DEBUG,
                (buffer, context) -> SetCollisionDebugS2CPacket.handle(
                        SetCollisionDebugS2CPacket.decode(buffer), context));
        *///?} else {
        NetworkManager.registerReceiver(
                NetworkManager.s2c(),
                SetCollisionDebugS2CPacket.TYPE,
                SetCollisionDebugS2CPacket.STREAM_CODEC,
                SetCollisionDebugS2CPacket::handle);
        //?}
    }

    public static void sendToPlayer(ServerPlayer player, SetCollisionDebugS2CPacket packet) {
        //? if forge {
        /*FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        SetCollisionDebugS2CPacket.encode(packet, buffer);
        NetworkManager.sendToPlayer(player, SET_COLLISION_DEBUG, buffer);
        *///?} else {
        NetworkManager.sendToPlayer(player, (CustomPacketPayload) packet);
        //?}
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Axiomata.MOD_ID, path);
    }
}
