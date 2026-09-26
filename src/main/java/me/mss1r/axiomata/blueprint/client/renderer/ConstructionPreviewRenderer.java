package me.mss1r.axiomata.blueprint.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.mss1r.axiomata.blueprint.internal.construction.ConstructionPlacementHelper;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ConstructionPreviewRenderer {
    private ConstructionPreviewRenderer() {
    }

    public static void renderPreview(
            Minecraft minecraft,
            Level level,
            ItemStack stack,
            BlueprintDefinition recipe,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            Vec3 worldPos,
            Vec3 renderPos,
            float yaw,
            float partialTick,
            float alpha,
            int renderSeed
    ) {
        if (stack.isEmpty()) {
            return;
        }

        if (renderEntityPreview(minecraft, level, stack, recipe, poseStack, buffer, packedLight, worldPos, renderPos, yaw, partialTick, alpha)) {
            return;
        }

        if (renderBlockPreview(minecraft, level, stack, poseStack, buffer, packedLight, worldPos, renderPos, yaw, alpha, renderSeed)) {
            return;
        }

        renderItemPreview(minecraft, level, stack, poseStack, buffer, packedLight, renderPos, yaw, alpha, renderSeed);
    }

    private static boolean renderEntityPreview(
            Minecraft minecraft,
            Level level,
            ItemStack stack,
            BlueprintDefinition recipe,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            Vec3 worldPos,
            Vec3 renderPos,
            float yaw,
            float partialTick,
            float alpha
    ) {
        Entity previewEntity = ConstructionPlacementHelper.createPreviewEntity(
                level, stack, worldPos, yaw, minecraft.player == null ? 0 : minecraft.player.tickCount, recipe);
        if (previewEntity == null) {
            return false;
        }

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0.82F, 0.92F, 1.0F, alpha);
        dispatcher.setRenderShadow(false);
        try {
            dispatcher.render(previewEntity, renderPos.x, renderPos.y, renderPos.z, yaw, partialTick, poseStack, buffer, packedLight);
        } finally {
            dispatcher.setRenderShadow(true);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }

        return true;
    }

    private static boolean renderBlockPreview(
            Minecraft minecraft,
            Level level,
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            Vec3 worldPos,
            Vec3 renderPos,
            float yaw,
            float alpha,
            int renderSeed
    ) {
        BlockState state = ConstructionPlacementHelper.createPreviewBlockState(stack, yaw);
        if (state == null) {
            return false;
        }

        BlockPos blockPos = BlockPos.containing(worldPos.x, worldPos.y - 0.05D, worldPos.z);
        poseStack.pushPose();
        poseStack.translate(
                renderPos.x - (worldPos.x - blockPos.getX()),
                renderPos.y - (worldPos.y - blockPos.getY()),
                renderPos.z - (worldPos.z - blockPos.getZ())
        );

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0.82F, 0.92F, 1.0F, alpha);
        try {
            if (state.getRenderShape() == RenderShape.MODEL) {
                minecraft.getBlockRenderer().renderSingleBlock(state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
            } else {
                poseStack.translate(0.5D, 0.5D, 0.5D);
                poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
                poseStack.scale(1.65F, 1.65F, 1.65F);
                minecraft.getItemRenderer().renderStatic(
                        stack,
                        ItemDisplayContext.FIXED,
                        packedLight,
                        OverlayTexture.NO_OVERLAY,
                        poseStack,
                        buffer,
                        level,
                        renderSeed
                );
            }
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            poseStack.popPose();
        }

        return true;
    }

    private static void renderItemPreview(
            Minecraft minecraft,
            Level level,
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            Vec3 renderPos,
            float yaw,
            float alpha,
            int renderSeed
    ) {
        poseStack.pushPose();
        poseStack.translate(renderPos.x, renderPos.y + 0.55D, renderPos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.scale(1.35F, 1.35F, 1.35F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0.82F, 0.92F, 1.0F, alpha);
        minecraft.getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                level,
                renderSeed
        );
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        poseStack.popPose();
    }
}
