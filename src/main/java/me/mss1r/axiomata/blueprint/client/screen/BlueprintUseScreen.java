package me.mss1r.axiomata.blueprint.client.screen;

import me.mss1r.axiomata.blueprint.client.PlacementTargetHelper;
import me.mss1r.axiomata.blueprint.internal.construction.ConstructionPlacementHelper;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.menu.BlueprintUseMenu;
import me.mss1r.axiomata.blueprint.network.C2SUseBlueprintPacket;
import me.mss1r.axiomata.blueprint.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BlueprintUseScreen extends AbstractContainerScreen<BlueprintUseMenu> {
    private static final int ITEM_SIZE = 16;
    private static final int COLS = 4;
    private static final int COL_SPACING = 32;
    private static final int ROW_SPACING = 22;
    private final BlueprintDefinition recipe;
    private final List<Map.Entry<ItemStack, Integer>> ingredientsDisplay = new ArrayList<>();
    private int centerX;
    private int centerY;
    private int btnY;
    @Nullable
    private Button actionButton;

    public BlueprintUseScreen(BlueprintUseMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.recipe = menu.getRecipe();
        this.imageWidth = 0;
        this.imageHeight = 0;

        if (recipe != null && recipe.key != null) {
            Map<Item, Integer> aggregated = new LinkedHashMap<>();
            for (BlueprintDefinition.IngredientSpec spec : recipe.key.values()) {
                Item item = getRegisteredItem(spec.item);
                if (item != null) {
                    aggregated.merge(item, Math.max(1, spec.count), Integer::sum);
                }
            }

            for (Map.Entry<Item, Integer> entry : aggregated.entrySet()) {
                ingredientsDisplay.add(Map.entry(new ItemStack(entry.getKey()), entry.getValue()));
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        centerX = this.width / 2;
        centerY = this.height / 2;
        btnY = centerY - 10;

        Component buttonText = recipe != null && recipe.buildsInWorld()
                ? Component.translatable("gui.axiomata.start_construction")
                : Component.translatable("gui.axiomata.use_blueprint");

        this.actionButton = this.addRenderableWidget(new Button.Builder(buttonText, btn -> {
            NetworkHandler.sendToServer(createUsePacket());
            this.onClose();
        }).pos(centerX - 70, btnY).size(140, 22).build());
        updateActionButtonState();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        //? if forge {
        /*this.renderBackground(guiGraphics);
        *///?} else {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        //?}
        updateActionButtonState();

        String needText = Component.translatable("gui.axiomata.requires").getString();
        int needTextY = 16;
        int needTextX = this.width - 5 - this.font.width(needText);
        guiGraphics.drawString(this.font, needText, needTextX, needTextY, 0xFFFFFF, false);

        int n = ingredientsDisplay.size();
        int gridWidth = Math.min(n, COLS) * COL_SPACING - ((n > 0) ? (COL_SPACING - ITEM_SIZE) : 0);

        int gridStartX = needTextX + this.font.width(needText) / 2 - gridWidth / 2;
        int gridStartY = needTextY + 16;

        for (int i = 0; i < n; i++) {
            int col = i % COLS;
            int row = i / COLS;

            int itemX = gridStartX + col * COL_SPACING;
            int itemY = gridStartY + row * ROW_SPACING;

            Map.Entry<ItemStack, Integer> entry = ingredientsDisplay.get(i);
            ItemStack stack = entry.getKey();
            int count = entry.getValue();

            guiGraphics.renderItem(stack, itemX, itemY);
            guiGraphics.drawString(this.font, String.valueOf(count), itemX + ITEM_SIZE + 4, itemY + 6, 0xFFFFFF, false);

            if (isHovering(itemX, itemY, ITEM_SIZE, ITEM_SIZE, mouseX, mouseY)) {
                guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY);
            }
        }

        ItemStack resultStack = getResultStack();
        if (!resultStack.isEmpty() && recipe != null) {
            String getText = recipe.buildsInWorld()
                    ? Component.translatable("gui.axiomata.result_after_construction").getString()
                    : Component.translatable("gui.axiomata.you_receive").getString();
            int labelWidth = this.font.width(getText);
            int getY = btnY + 28;
            guiGraphics.drawString(this.font, getText, centerX - labelWidth / 2, getY, 0xAAAAAA, false);
            guiGraphics.renderItem(resultStack, centerX - 8, getY + 12);
            if (isHovering(centerX - 8, getY + 12, ITEM_SIZE, ITEM_SIZE, mouseX, mouseY)) {
                guiGraphics.renderTooltip(this.font, resultStack, mouseX, mouseY);
            }

            if (recipe.buildsInWorld()) {
                String deployText = Component.translatable("gui.axiomata.builds_in_world").getString();
                int deployWidth = this.font.width(deployText);
                guiGraphics.drawString(this.font, deployText, centerX - deployWidth / 2, getY + 32, 0x9AC4FF, false);

                PreviewState previewState = getPreviewState();
                String previewText = Component.translatable(
                        switch (previewState) {
                            case READY -> "gui.axiomata.preview_target_ready";
                            case INVALID -> "gui.axiomata.preview_target_invalid";
                            case MISSING -> "gui.axiomata.preview_target_missing";
                        }
                ).getString();
                int previewWidth = this.font.width(previewText);
                guiGraphics.drawString(
                        this.font,
                        previewText,
                        centerX - previewWidth / 2,
                        getY + 44,
                        switch (previewState) {
                            case READY -> 0x89F0A1;
                            case INVALID -> 0xFF7474;
                            case MISSING -> 0xE7B46A;
                        },
                        false
                );
            }
        }

        for (var widget : this.renderables) {
            if (widget instanceof Button btn) {
                btn.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private C2SUseBlueprintPacket createUsePacket() {
        return new C2SUseBlueprintPacket(menu.getRecipeId());
    }

    @Nullable
    private ConstructionPlacementHelper.PlacementPlan getPlacementPlan() {
        Minecraft minecraft = this.minecraft;
        ItemStack resultStack = getResultStack();
        if (minecraft == null || minecraft.level == null || resultStack.isEmpty()) {
            return null;
        }

        BlockHitResult blockHitResult = PlacementTargetHelper.getPlacementTarget(minecraft, resultStack, recipe, 0.0F);
        if (blockHitResult == null) {
            return null;
        }

        return ConstructionPlacementHelper.findPlacement(
                minecraft.level,
                resultStack,
                blockHitResult.getBlockPos(),
                blockHitResult.getDirection(),
                blockHitResult.getLocation(),
                minecraft.player == null ? 0.0F : minecraft.player.getYRot(),
                recipe
        );
    }

    private PreviewState getPreviewState() {
        if (this.minecraft == null || getResultStack().isEmpty()
                || PlacementTargetHelper.getPlacementTarget(this.minecraft, getResultStack(), recipe, 0.0F) == null) {
            return PreviewState.MISSING;
        }

        ConstructionPlacementHelper.PlacementPlan plan = getPlacementPlan();
        if (plan == null || !plan.valid()) {
            return PreviewState.INVALID;
        }

        return PreviewState.READY;
    }

    private void updateActionButtonState() {
        if (this.actionButton != null) {
            this.actionButton.active = recipe == null || !recipe.buildsInWorld() || getPreviewState() == PreviewState.READY;
        }
    }

    private ItemStack getResultStack() {
        if (recipe == null || recipe.result == null || recipe.result.item == null) {
            return ItemStack.EMPTY;
        }

        Item item = getRegisteredItem(recipe.result.item);
        return item == null ? ItemStack.EMPTY : new ItemStack(item, recipe.result.count);
    }

    @Nullable
    private static Item getRegisteredItem(String itemId) {
        ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);
        return resourceLocation != null && BuiltInRegistries.ITEM.containsKey(resourceLocation)
                ? BuiltInRegistries.ITEM.get(resourceLocation)
                : null;
    }

    private enum PreviewState {
        MISSING,
        INVALID,
        READY
    }
}
