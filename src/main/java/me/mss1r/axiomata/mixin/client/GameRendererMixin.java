package me.mss1r.axiomata.mixin.client;

import me.mss1r.axiomata.collision.system.StructureInteractionPicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "pick", at = @At("TAIL"))
    private void axiomata$pickStructureGeometry(float partialTick, CallbackInfo callbackInfo) {
        Entity viewer = minecraft.getCameraEntity();
        if (viewer == null || minecraft.level == null || minecraft.player == null) {
            return;
        }

        double reach = interactionRange(minecraft.player);
        Vec3 start = viewer.getEyePosition(partialTick);
        Vec3 end = start.add(viewer.getViewVector(1.0F).scale(reach));
        double closestDistanceSqr = reach * reach;
        HitResult currentHit = minecraft.hitResult;
        if (currentHit != null && currentHit.getType() != HitResult.Type.MISS) {
            closestDistanceSqr = Math.min(closestDistanceSqr, start.distanceToSqr(currentHit.getLocation()));
        }

        EntityHitResult structureHit = StructureInteractionPicker.findHit(
                viewer, start, end,
                entity -> !entity.isSpectator() && entity.isPickable(),
                closestDistanceSqr);
        if (structureHit == null) {
            return;
        }

        minecraft.hitResult = structureHit;
        Entity target = structureHit.getEntity();
        minecraft.crosshairPickEntity = target instanceof LivingEntity || target instanceof ItemFrame
                ? target
                : null;
    }

    private static double interactionRange(net.minecraft.world.entity.player.Player player) {
        //? if forge {
        /*return player.getEntityReach();
        *///?} else {
        return player.entityInteractionRange();
        //?}
    }
}
