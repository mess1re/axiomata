package me.mss1r.axiomata.mixin;

import me.mss1r.axiomata.platform.AxiomataPlatform;

import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.system.StructureInteractionPicker;
import me.mss1r.axiomata.collision.system.VirtualPlatformSupport;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
//? if >=1.21 {
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.phys.AABB;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Shadow
    private boolean clientIsFloating;

    @Inject(method = "tick", at = @At("HEAD"))
    private void axiomata$allowVirtualPlatformSupport(CallbackInfo callbackInfo) {
        if (VirtualPlatformSupport.isSupported(player)) {
            clientIsFloating = false;
        }
    }

    @Redirect(method = "handleInteract", at = @At(value = "INVOKE",
            //? if <1.21 {
            /*target = "Lnet/minecraft/server/level/ServerPlayer;canReachRaw(Lnet/minecraft/world/entity/Entity;D)Z",
            remap = false
            *///?} else {
            target = "Lnet/minecraft/server/level/ServerPlayer;canInteractWithEntity(Lnet/minecraft/world/phys/AABB;D)Z"
            //?}
    ))
    //? if <1.21 {
    /*private boolean axiomata$validateStructureReach(ServerPlayer serverPlayer, Entity target, double padding) {
        if (AxiomataPlatform.canReach(serverPlayer, target, padding)) {
            return true;
        }
    *///?} else {
    private boolean axiomata$validateStructureReach(ServerPlayer serverPlayer, AABB bounds, double padding,
                                                    ServerboundInteractPacket packet) {
        if (serverPlayer.canInteractWithEntity(bounds, padding)) {
            return true;
        }
        Entity target = packet.getTarget(serverPlayer.serverLevel());
        if (target == null) {
            return false;
        }
    //?}
        return target instanceof CollidableStructure structure
                && StructureInteractionPicker.isWithinReach(
                        serverPlayer, structure, interactionRange(serverPlayer) + padding);
    }

    private static double interactionRange(ServerPlayer player) {
        //? if <1.21 {
        /*return AxiomataPlatform.entityReach(player);
        *///?} else {
        return player.entityInteractionRange();
        //?}
    }
}
