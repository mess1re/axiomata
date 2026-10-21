package me.mss1r.axiomata.mixin.client;

import me.mss1r.axiomata.client.ClimbPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {
    @Unique
    private boolean axiomata$climbPosed;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void axiomata$resetClimbPose(T entity, float limbAngle, float limbDistance, float age,
                                         float headYaw, float headPitch, CallbackInfo ci) {
        if (axiomata$climbPosed) {
            ClimbPose.reset((HumanoidModel<?>) (Object) this);
            axiomata$climbPosed = false;
        }
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void axiomata$applyClimbPose(T entity, float limbAngle, float limbDistance, float age,
                                         float headYaw, float headPitch, CallbackInfo ci) {
        // The first-person hand is set up with all-zero arguments; it keeps the vanilla arm.
        if (age == 0.0F && headYaw == 0.0F && headPitch == 0.0F && limbDistance == 0.0F) {
            return;
        }
        axiomata$climbPosed = ClimbPose.apply((HumanoidModel<?>) (Object) this, entity, age, headYaw, headPitch);
    }
}
