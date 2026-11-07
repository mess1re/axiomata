package me.mss1r.axiomata.forge.mixin;

import me.mss1r.axiomata.platform.AxiomataPlatform;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.system.StructureInteractionPicker;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerStructureReachMixin {
    @Shadow public ServerPlayer player;

    @Unique private static final double AXIOMATA_INTERACTION_PADDING = 3.0D;

    // Forge and Mohist check reach before dispatch. Arclight replaces this part of the handler.
    @Redirect(method = "handleInteract", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;canReachRaw(Lnet/minecraft/world/entity/Entity;D)Z",
            remap = false), require = 0)
    private boolean axiomata$validateStructureReach(ServerPlayer player, Entity target, double padding) {
        return AxiomataPlatform.canReach(player, target, padding)
                || target instanceof CollidableStructure structure
                    && StructureInteractionPicker.isWithinReach(player, structure,
                        AxiomataPlatform.entityReach(player) + padding);
    }

    // Keep the range check before packet dispatch, including on servers that replace the Forge check.
    @Inject(method = "handleInteract", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ServerboundInteractPacket;dispatch(Lnet/minecraft/network/protocol/game/ServerboundInteractPacket$Handler;)V"),
            cancellable = true)
    private void axiomata$checkStructureBeforeDispatch(ServerboundInteractPacket packet, CallbackInfo ci) {
        Entity target = packet.getTarget(player.serverLevel());
        if (target instanceof CollidableStructure
                && !axiomata$validateStructureReach(player, target, AXIOMATA_INTERACTION_PADDING)) {
            ci.cancel();
        }
    }
}
