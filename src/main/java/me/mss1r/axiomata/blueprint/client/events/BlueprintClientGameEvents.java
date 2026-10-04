package me.mss1r.axiomata.blueprint.client.events;

import com.mojang.blaze3d.vertex.VertexConsumer;
import me.mss1r.axiomata.blueprint.api.BlueprintTags;
import me.mss1r.axiomata.blueprint.client.PlacementTargetHelper;
import me.mss1r.axiomata.blueprint.client.renderer.ConstructionPreviewRenderer;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.internal.construction.ConstructionPlacementHelper;
import me.mss1r.axiomata.blueprint.api.ConstructionStarters;
import me.mss1r.axiomata.blueprint.item.BlueprintItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import net.minecraftforge.client.event.RenderLevelStageEvent;
*///?} else {
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
//?}

public class BlueprintClientGameEvents {
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }

        ItemStack blueprint = getHeldBlueprint(player);
        if (blueprint.isEmpty()) {
            return;
        }

        String recipeId = ConstructionStarters.definitionOf(blueprint);
        BlueprintDefinition recipe = recipeId == null ? null : BlueprintDefinitions.get(recipeId);
        if (recipe == null || !recipe.buildsInWorld()) {
            return;
        }

        // The result with its data, so the preview takes the room the finished build will.
        ItemStack previewStack = BlueprintItem.createResultStack(recipe);
        if (previewStack.isEmpty()) {
            return;
        }
        //? if forge {
        /*float partialTick = event.getPartialTick();
        *///?} else {
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        //?}
        BlockHitResult blockHitResult = PlacementTargetHelper.getPlacementTarget(minecraft, previewStack, recipe, partialTick);
        if (blockHitResult == null) {
            return;
        }
        ConstructionPlacementHelper.PlacementPlan plan = ConstructionPlacementHelper.findPlacement(
                level,
                previewStack,
                blockHitResult.getBlockPos(),
                blockHitResult.getDirection(),
                blockHitResult.getLocation(),
                player.getYRot(),
                recipe
        );
        if (plan == null) {
            return;
        }

        Vec3 cameraPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        ConstructionPreviewRenderer.renderPreview(
                minecraft,
                level,
                previewStack,
                recipe,
                event.getPoseStack(),
                bufferSource,
                0xF000F0,
                plan.worldPos(),
                new Vec3(
                        plan.worldPos().x - cameraPos.x,
                        plan.worldPos().y - cameraPos.y,
                        plan.worldPos().z - cameraPos.z
                ),
                player.getYRot(),
                partialTick,
                plan.valid() ? 0.45F : 0.30F,
                0
        );

        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(
                event.getPoseStack(),
                lineConsumer,
                plan.bounds().move(-cameraPos.x, -cameraPos.y, -cameraPos.z),
                plan.valid() ? 0.45F : 1.0F,
                plan.valid() ? 0.8F : 0.2F,
                plan.valid() ? 1.0F : 0.2F,
                0.85F
        );

        bufferSource.endBatch();
    }

    private static ItemStack getHeldBlueprint(LocalPlayer player) {
        if (!player.getMainHandItem().is(BlueprintTags.CONSTRUCTION_HAMMERS)) {
            return ItemStack.EMPTY;
        }

        ItemStack offHand = player.getOffhandItem();
        return ConstructionStarters.definitionOf(offHand) != null ? offHand : ItemStack.EMPTY;
    }
}
