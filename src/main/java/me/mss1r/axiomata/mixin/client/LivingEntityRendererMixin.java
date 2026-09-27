package me.mss1r.axiomata.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.mss1r.axiomata.collision.system.StructureClimbingSystem;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity> {
    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void axiomata$faceClimbedStructure(T entity, PoseStack poseStack, float ageInTicks,
                                               float rotationYaw, float partialTicks,
                                               //? if neoforge {
                                               float scale,
                                               //?}
                                               CallbackInfo callbackInfo) {
        if (entity.getVehicle() != null) {
            return;
        }
        StructureClimbingSystem.ClimbPoseState climb =
                StructureClimbingSystem.getClimbPoseState(entity, partialTicks);
        if (climb.weight() <= 0.0F) {
            return;
        }

        float correction = Mth.wrapDegrees(rotationYaw - climb.uphillYawDegrees()) * climb.weight();
        poseStack.mulPose(Axis.YP.rotationDegrees(correction));
    }
}
