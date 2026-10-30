package me.mss1r.axiomata.forge.mixin;

import me.mss1r.axiomata.platform.AxiomataPlatform;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.system.StructureInteractionPicker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Forge replaces vanilla's reach check; intercept that extension only on Forge. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerStructureReachMixin {
    @Redirect(method = "handleInteract", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;canReachRaw(Lnet/minecraft/world/entity/Entity;D)Z",
            remap = false))
    private boolean axiomata$validateStructureReach(ServerPlayer player, Entity target, double padding) {
        return AxiomataPlatform.canReach(player, target, padding)
                || target instanceof CollidableStructure structure
                    && StructureInteractionPicker.isWithinReach(player, structure,
                        AxiomataPlatform.entityReach(player) + padding);
    }
}
