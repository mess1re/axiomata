package me.mss1r.axiomata.fabric.mixin;

import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.system.StructureInteractionPicker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Only configured on 1.20.1; 1.21 uses the vanilla reach redirect in common. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerStructureReachMixin {
    @Shadow public ServerPlayer player;
    //? if <1.21 {
    /*@Redirect(method = "handleInteract", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/AABB;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"))
    private double axiomata$surfaceReach(AABB bounds, Vec3 eye, ServerboundInteractPacket packet) {
        double original = bounds.distanceToSqr(eye);
        var target = packet.getTarget(player.serverLevel());
        if (original >= ServerGamePacketListenerImpl.MAX_INTERACTION_DISTANCE
                && target instanceof CollidableStructure structure
                && StructureInteractionPicker.isWithinReach(player, structure,
                    Math.sqrt(ServerGamePacketListenerImpl.MAX_INTERACTION_DISTANCE))) return 0.0D;
        return original;
    }
    *///?}
}
