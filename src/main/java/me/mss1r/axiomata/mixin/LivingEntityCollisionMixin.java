package me.mss1r.axiomata.mixin;

import me.mss1r.axiomata.collision.system.StructureClimbingSystem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityCollisionMixin {
    @Shadow
    protected boolean jumping;

    @Inject(method = "handleOnClimbable", at = @At("HEAD"), cancellable = true)
    private void axiomata$climbStructures(Vec3 movement, CallbackInfoReturnable<Vec3> callbackInfo) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!StructureClimbingSystem.isOnClimbable(entity)) {
            return;
        }

        Vec3 adjusted = entity instanceof Player player
                ? StructureClimbingSystem.applyPlayerClimbableVelocity(player, movement, jumping)
                : StructureClimbingSystem.applyClimbableVelocity(entity, movement);
        callbackInfo.setReturnValue(adjusted);
    }
}
