package me.mss1r.axiomata.blueprint.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.mss1r.axiomata.blueprint.internal.construction.ConstructionPlacementHelper;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

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
            float alpha
    ) {
        Entity previewEntity = ConstructionPlacementHelper.createPreviewEntity(
                level, stack, worldPos, yaw, minecraft.player == null ? 0 : minecraft.player.tickCount, recipe);
        if (previewEntity == null) {
            return;
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
    }
}
