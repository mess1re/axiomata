package me.mss1r.axiomata.blueprint.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.blueprint.BlueprintModule;
import me.mss1r.axiomata.blueprint.blockentity.DrawingTableBlockEntity;
import me.mss1r.axiomata.blueprint.tracing.BlueprintOutline;
import me.mss1r.axiomata.blueprint.tracing.OutlineCatalog;
import me.mss1r.axiomata.blueprint.tracing.TracingSession;
import me.mss1r.axiomata.blueprint.tracing.TracingState;
import me.mss1r.axiomata.blueprint.client.ClientTracing;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.item.BlueprintItem;
import me.mss1r.axiomata.blueprint.menu.DrawingTableLayout;
import me.mss1r.axiomata.blueprint.menu.DrawingTableMenu;
import me.mss1r.axiomata.blueprint.network.C2SSelectBlueprintPacket;
import me.mss1r.axiomata.blueprint.network.C2SSetTableSlotPacket;
import me.mss1r.axiomata.blueprint.network.C2STakeResultFromTablePacket;
import me.mss1r.axiomata.blueprint.network.NetworkHandler;
import me.mss1r.axiomata.blueprint.registry.BlueprintItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// The layout uses its own 640x320 coordinate space and is scaled as a unit. Mouse coordinates are
// converted back into that space as well; vanilla slot coordinates are intentionally not used.
public class DrawingTableScreen extends AbstractContainerScreen<DrawingTableMenu> {
    private static final ResourceLocation BACKGROUND =
            ResourceIds.id(BlueprintModule.MOD_ID, "textures/gui/background.png");
    private static final ResourceLocation PAPER =
            ResourceIds.id(BlueprintModule.MOD_ID, "textures/gui/paper.png");
    private static final ResourceLocation SLOT =
            ResourceIds.id(BlueprintModule.MOD_ID, "textures/gui/slot.png");
    private static final ResourceLocation QUILL =
            ResourceIds.id(BlueprintModule.MOD_ID, "textures/gui/quill.png");
    private static final ResourceLocation PANEL_LIST =
            ResourceIds.id(BlueprintModule.MOD_ID, "textures/gui/panel_list.png");
    private static final ResourceLocation PANEL_MATERIALS =
            ResourceIds.id(BlueprintModule.MOD_ID, "textures/gui/panel_materials.png");
    private static final ResourceLocation GROOVE =
            ResourceIds.id(BlueprintModule.MOD_ID, "textures/gui/groove.png");
    private static final ResourceLocation FILL_PROGRESS =
            ResourceIds.id(BlueprintModule.MOD_ID, "textures/gui/fill_progress.png");
    private static final ResourceLocation FILL_WANDER =
            ResourceIds.id(BlueprintModule.MOD_ID, "textures/gui/fill_wander.png");
    private static final ResourceLocation ARROW =
            ResourceIds.id(BlueprintModule.MOD_ID, "textures/gui/arrow.png");

    private static final int QUILL_NIB_X = 1;
    private static final int QUILL_NIB_Y = 14;

    private static final int COLOUR_TEXT = 0xFFE8DFC8;
    private static final int COLOUR_TEXT_DIM = 0xFF9A8B6B;
    private static final int COLOUR_GUIDE = 0xFF141414;
    private static final int COLOUR_INK = 0xFF33488C;
    private static final int COLOUR_SELECTED = 0x66E8DFC8;
    private static final int COLOUR_PLATE = 0xFF2A1C0F;
    private static final int COLOUR_PROGRESS = 0xFF6E8F4A;
    private static final int COLOUR_WANDER = 0xFF8F4A4A;
    private static final int COLOUR_HOVER = 0x80FFFFFF;

    private final List<Entry> entries = new ArrayList<>();
    private float scale = 1.0F;
    private int offsetX;
    private int offsetY;
    private int scrollIndex;
    private boolean drawing;

    private record Entry(String id, ItemStack icon, Component name) {
    }

    public DrawingTableScreen(DrawingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = DrawingTableLayout.WIDTH;
        this.imageHeight = DrawingTableLayout.HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        scale = Math.min(1.0F, Math.min((float) this.width / DrawingTableLayout.WIDTH,
                (float) this.height / DrawingTableLayout.HEIGHT));
        offsetX = Math.round((this.width - DrawingTableLayout.WIDTH * scale) / 2.0F);
        offsetY = Math.round((this.height - DrawingTableLayout.HEIGHT * scale) / 2.0F);
        rebuildEntries();
    }

    private void rebuildEntries() {
        entries.clear();
        for (Map.Entry<String, BlueprintDefinition> entry : BlueprintDefinitions.allById().entrySet()) {
            if (!OutlineCatalog.isDrawable(entry.getValue().outlineId(entry.getKey()))) {
                continue;
            }
            ItemStack icon = BlueprintItem.createResultStack(entry.getValue());
            entries.add(new Entry(entry.getKey(), icon,
                    icon.isEmpty() ? Component.literal(entry.getKey()) : icon.getHoverName()));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ClientTracing.flush();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        //? if forge {
        /*renderBackground(guiGraphics);
        *///?} else {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        //?}

        double localX = localX(mouseX);
        double localY = localY(mouseY);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(offsetX, offsetY, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);

        guiGraphics.blit(BACKGROUND, 0, 0, 0, 0,
                DrawingTableLayout.WIDTH, DrawingTableLayout.HEIGHT,
                DrawingTableLayout.WIDTH, DrawingTableLayout.HEIGHT);

        guiGraphics.blit(PANEL_LIST, DrawingTableLayout.LIST_PANEL_X, DrawingTableLayout.LIST_PANEL_Y, 0, 0,
                DrawingTableLayout.LIST_PANEL_WIDTH, DrawingTableLayout.LIST_PANEL_HEIGHT,
                DrawingTableLayout.LIST_PANEL_WIDTH, DrawingTableLayout.LIST_PANEL_HEIGHT);
        guiGraphics.blit(PANEL_MATERIALS, DrawingTableLayout.MATERIALS_PANEL_X, DrawingTableLayout.MATERIALS_PANEL_Y,
                0, 0, DrawingTableLayout.MATERIALS_PANEL_WIDTH, DrawingTableLayout.MATERIALS_PANEL_HEIGHT,
                DrawingTableLayout.MATERIALS_PANEL_WIDTH, DrawingTableLayout.MATERIALS_PANEL_HEIGHT);

        renderList(guiGraphics, localX, localY);
        renderSheet(guiGraphics, localX, localY);
        renderPanel(guiGraphics, localX, localY);

        guiGraphics.pose().popPose();

        renderSlotTooltip(guiGraphics, localX, localY, mouseX, mouseY);
    }

    private void renderSlotTooltip(GuiGraphics guiGraphics, double localX, double localY, int mouseX, int mouseY) {
        if (frameClicked(localX, localY, DrawingTableLayout.RESULT_FRAME_X)) {
            ItemStack result = menu.getContainer().getItem(DrawingTableBlockEntity.SLOT_RESULT);
            if (!result.isEmpty()) {
                guiGraphics.renderTooltip(this.font, result, mouseX, mouseY);
            }
            return;
        }
        if (frameClicked(localX, localY, DrawingTableLayout.PAPER_FRAME_X)) {
            renderSlotHint(guiGraphics, DrawingTableBlockEntity.SLOT_PAPER, "paper", mouseX, mouseY);
        } else if (frameClicked(localX, localY, DrawingTableLayout.INK_FRAME_X)) {
            renderSlotHint(guiGraphics, DrawingTableBlockEntity.SLOT_INK, "ink", mouseX, mouseY);
        }
    }

    private void renderSlotHint(GuiGraphics guiGraphics, int slot, String name, int mouseX, int mouseY) {
        boolean filled = !menu.getContainer().getItem(slot).isEmpty();
        guiGraphics.renderComponentTooltip(this.font, List.of(
                Component.translatable("gui.axiomata.drawing_table.slot." + name),
                Component.translatable(filled
                        ? "gui.axiomata.drawing_table.slot.take"
                        : "gui.axiomata.drawing_table.slot.put").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)
        ), mouseX, mouseY);
    }

    private boolean listLocked() {
        TracingSession session = ClientTracing.session();
        return session != null && session.coverage() > 0.0F;
    }

    private void renderList(GuiGraphics guiGraphics, double mouseX, double mouseY) {
        int rows = visibleRows();
        String selected = ClientTracing.blueprintId();
        boolean locked = listLocked();
        for (int row = 0; row < rows && row + scrollIndex < entries.size(); row++) {
            Entry entry = entries.get(row + scrollIndex);
            int x = DrawingTableLayout.listContentX();
            int y = DrawingTableLayout.listContentY() + row * DrawingTableLayout.LIST_ROW_HEIGHT;
            boolean chosen = entry.id().equals(selected);
            boolean hovered = !locked && inside(mouseX, mouseY, x, y,
                    DrawingTableLayout.listContentWidth(), DrawingTableLayout.LIST_ROW_HEIGHT);
            if (chosen || hovered) {
                guiGraphics.fill(x, y, x + DrawingTableLayout.listContentWidth(),
                        y + DrawingTableLayout.LIST_ROW_HEIGHT - 1, COLOUR_SELECTED);
            }
            guiGraphics.renderItem(entry.icon(), x + 1, y + 1);
            guiGraphics.drawString(this.font, trimmed(entry.name()), x + 20, y + 6,
                    locked && !chosen ? COLOUR_TEXT_DIM : COLOUR_TEXT, false);
        }
    }

    private Component trimmed(Component name) {
        String text = name.getString();
        int room = DrawingTableLayout.listContentWidth() - 22;
        return this.font.width(text) <= room
                ? name
                : Component.literal(this.font.plainSubstrByWidth(text, room - 6) + "...");
    }

    private int visibleRows() {
        return DrawingTableLayout.listRows();
    }

    private void renderSheet(GuiGraphics guiGraphics, double mouseX, double mouseY) {
        guiGraphics.blit(PAPER, DrawingTableLayout.SHEET_X, DrawingTableLayout.SHEET_Y, 0, 0,
                DrawingTableLayout.SHEET_SIZE, DrawingTableLayout.SHEET_SIZE,
                DrawingTableLayout.SHEET_SIZE, DrawingTableLayout.SHEET_SIZE);

        TracingState state = menu.state();
        BlueprintOutline outline = ClientTracing.outline();
        if (outline == null) {
            guiGraphics.drawCenteredString(this.font,
                    Component.translatable("gui.axiomata.drawing_table.pick"),
                    DrawingTableLayout.SHEET_X + DrawingTableLayout.SHEET_SIZE / 2,
                    DrawingTableLayout.SHEET_Y + DrawingTableLayout.SHEET_SIZE / 2, COLOUR_TEXT_DIM);
            return;
        }

        RenderSystem.enableBlend();
        setTint(guiGraphics, COLOUR_GUIDE);
        guiGraphics.blit(outline.texture(), DrawingTableLayout.SHEET_X, DrawingTableLayout.SHEET_Y, 0, 0,
                DrawingTableLayout.SHEET_SIZE, DrawingTableLayout.SHEET_SIZE,
                DrawingTableLayout.SHEET_SIZE, DrawingTableLayout.SHEET_SIZE);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();

        renderInk(guiGraphics);

        if (state == TracingState.RUINED) {
            guiGraphics.drawCenteredString(this.font,
                    Component.translatable("gui.axiomata.drawing_table.ruined"),
                    DrawingTableLayout.SHEET_X + DrawingTableLayout.SHEET_SIZE / 2,
                    DrawingTableLayout.SHEET_Y + DrawingTableLayout.SHEET_SIZE / 2, COLOUR_WANDER);
        } else if (overSheet(mouseX, mouseY)) {
            guiGraphics.blit(QUILL, (int) mouseX - QUILL_NIB_X, (int) mouseY - QUILL_NIB_Y, 0, 0, 16, 16, 16, 16);
        }
    }

    private void renderInk(GuiGraphics guiGraphics) {
        TracingSession session = ClientTracing.session();
        if (session == null) {
            return;
        }
        int resolution = session.mask().resolution();
        int cell = DrawingTableLayout.SHEET_SIZE / resolution;
        for (int y = 0; y < resolution; y++) {
            int x = 0;
            while (x < resolution) {
                while (x < resolution && !session.covered(x, y)) {
                    x++;
                }
                int runStart = x;
                while (x < resolution && session.covered(x, y)) {
                    x++;
                }
                if (runStart == x) {
                    continue;
                }
                int left = DrawingTableLayout.SHEET_X + runStart * cell;
                int right = DrawingTableLayout.SHEET_X + x * cell;
                int top = DrawingTableLayout.SHEET_Y + y * cell;
                guiGraphics.fill(left, top, right, top + cell, COLOUR_INK);
            }
        }
    }

    private void renderPanel(GuiGraphics guiGraphics, double mouseX, double mouseY) {
        renderChosenName(guiGraphics);
        renderMaterials(guiGraphics);
        renderSlots(guiGraphics, mouseX, mouseY);
        renderProgress(guiGraphics);
    }

    private void renderChosenName(GuiGraphics guiGraphics) {
        for (Entry entry : entries) {
            if (entry.id().equals(ClientTracing.blueprintId())) {
                guiGraphics.drawString(this.font, entry.name(),
                        DrawingTableLayout.MATERIALS_PANEL_X + DrawingTableLayout.PANEL_BORDER + 1,
                        DrawingTableLayout.MATERIALS_PANEL_Y + DrawingTableLayout.PANEL_BORDER + 2,
                        COLOUR_TEXT, false);
                return;
            }
        }
    }

    private void renderMaterials(GuiGraphics guiGraphics) {
        BlueprintDefinition recipe = BlueprintDefinitions.get(ClientTracing.blueprintId());
        if (recipe == null) {
            return;
        }
        int index = 0;
        for (BlueprintDefinition.Material material : recipe.totals()) {
            int column = index % DrawingTableLayout.MATERIALS_COLUMNS;
            int row = index / DrawingTableLayout.MATERIALS_COLUMNS;
            int x = DrawingTableLayout.MATERIALS_PANEL_X + DrawingTableLayout.PANEL_BORDER
                    + column * DrawingTableLayout.MATERIALS_SPACING;
            int y = DrawingTableLayout.MATERIALS_PANEL_Y + 18 + row * DrawingTableLayout.MATERIALS_SPACING;
            ItemStack stack = material.displayStack();
            stack.setCount(material.count());
            guiGraphics.renderItem(stack, x, y);
            guiGraphics.renderItemDecorations(this.font, stack, x, y);
            index++;
        }
        renderStageCount(guiGraphics, recipe, index);
    }

    private void renderStageCount(GuiGraphics guiGraphics, BlueprintDefinition recipe, int materials) {
        if (recipe.stages().isEmpty()) {
            return;
        }
        int rows = (materials + DrawingTableLayout.MATERIALS_COLUMNS - 1) / DrawingTableLayout.MATERIALS_COLUMNS;
        guiGraphics.drawString(this.font,
                Component.translatable("gui.axiomata.drawing_table.stages", recipe.stageCount()),
                DrawingTableLayout.MATERIALS_PANEL_X + DrawingTableLayout.PANEL_BORDER + 1,
                DrawingTableLayout.MATERIALS_PANEL_Y + 22 + rows * DrawingTableLayout.MATERIALS_SPACING,
                COLOUR_TEXT_DIM, false);
    }

    private void renderSlots(GuiGraphics guiGraphics, double mouseX, double mouseY) {
        for (int frameX : new int[]{DrawingTableLayout.PAPER_FRAME_X, DrawingTableLayout.INK_FRAME_X,
                DrawingTableLayout.RESULT_FRAME_X}) {
            drawFrame(guiGraphics, frameX, DrawingTableLayout.SLOT_FRAME_Y);
            if (inside(mouseX, mouseY, frameX, DrawingTableLayout.SLOT_FRAME_Y,
                    DrawingTableLayout.FRAME_SIZE, DrawingTableLayout.FRAME_SIZE)) {
                guiGraphics.fill(frameX + 1, DrawingTableLayout.SLOT_FRAME_Y + 1,
                        frameX + DrawingTableLayout.FRAME_SIZE - 1,
                        DrawingTableLayout.SLOT_FRAME_Y + DrawingTableLayout.FRAME_SIZE - 1, COLOUR_HOVER);
            }
        }

        guiGraphics.blit(ARROW, DrawingTableLayout.ARROW_X, DrawingTableLayout.ARROW_Y, 0, 0,
                DrawingTableLayout.ARROW_WIDTH, DrawingTableLayout.ARROW_HEIGHT,
                DrawingTableLayout.ARROW_WIDTH, DrawingTableLayout.ARROW_HEIGHT);

        drawSlotItem(guiGraphics, DrawingTableBlockEntity.SLOT_PAPER, DrawingTableLayout.PAPER_FRAME_X);
        drawSlotItem(guiGraphics, DrawingTableBlockEntity.SLOT_INK, DrawingTableLayout.INK_FRAME_X);
        drawSlotItem(guiGraphics, DrawingTableBlockEntity.SLOT_RESULT, DrawingTableLayout.RESULT_FRAME_X);
    }

    private void drawSlotItem(GuiGraphics guiGraphics, int slot, int frameX) {
        ItemStack stack = menu.getContainer().getItem(slot);
        if (stack.isEmpty()) {
            return;
        }
        int x = DrawingTableLayout.slotX(frameX);
        int y = DrawingTableLayout.slotY();
        guiGraphics.renderItem(stack, x, y);
        guiGraphics.renderItemDecorations(this.font, stack, x, y);
    }

    private void drawFrame(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(SLOT, x, y, 0, 0,
                DrawingTableLayout.FRAME_SIZE, DrawingTableLayout.FRAME_SIZE,
                DrawingTableLayout.FRAME_SIZE, DrawingTableLayout.FRAME_SIZE);
    }

    private void renderProgress(GuiGraphics guiGraphics) {
        drawGroove(guiGraphics, DrawingTableLayout.PROGRESS_GROOVE_Y);
        float coverage = Math.min(1.0F, menu.coverage());
        drawFill(guiGraphics, FILL_PROGRESS, DrawingTableLayout.PROGRESS_GROOVE_Y, coverage);

        drawGroove(guiGraphics, DrawingTableLayout.WANDER_GROOVE_Y);
        int budget = menu.wanderBudget();
        if (budget > 0 && menu.wandered() > 0) {
            drawFill(guiGraphics, FILL_WANDER, DrawingTableLayout.WANDER_GROOVE_Y,
                    Math.min(1.0F, menu.wandered() / (float) budget));
        }

        renderSheetStatus(guiGraphics);
    }

    private void drawGroove(GuiGraphics guiGraphics, int y) {
        guiGraphics.blit(GROOVE, DrawingTableLayout.GROOVE_X, y, 0, 0,
                DrawingTableLayout.GROOVE_WIDTH, DrawingTableLayout.GROOVE_HEIGHT,
                DrawingTableLayout.GROOVE_WIDTH, DrawingTableLayout.GROOVE_HEIGHT);
    }

    private void drawFill(GuiGraphics guiGraphics, ResourceLocation texture, int grooveY, float fraction) {
        int width = Math.round(DrawingTableLayout.FILL_WIDTH * fraction);
        if (width <= 0) {
            return;
        }
        guiGraphics.blit(texture,
                DrawingTableLayout.GROOVE_X + DrawingTableLayout.FILL_INSET,
                grooveY + DrawingTableLayout.FILL_INSET,
                0, 0, width, DrawingTableLayout.FILL_HEIGHT,
                DrawingTableLayout.FILL_WIDTH, DrawingTableLayout.FILL_HEIGHT);
    }

    private void renderSheetStatus(GuiGraphics guiGraphics) {
        if (listLocked()) {
            guiGraphics.drawCenteredString(this.font,
                    Component.translatable("gui.axiomata.drawing_table.locked"),
                    DrawingTableLayout.SHEET_X + DrawingTableLayout.SHEET_SIZE / 2,
                    DrawingTableLayout.SHEET_Y + DrawingTableLayout.SHEET_SIZE + 14, COLOUR_TEXT_DIM);
        }
    }

    private void setTint(GuiGraphics guiGraphics, int colour) {
        guiGraphics.setColor((colour >> 16 & 0xFF) / 255.0F, (colour >> 8 & 0xFF) / 255.0F,
                (colour & 0xFF) / 255.0F, (colour >>> 24) / 255.0F);
    }

    private double localX(double mouseX) {
        return (mouseX - offsetX) / scale;
    }

    private double localY(double mouseY) {
        return (mouseY - offsetY) / scale;
    }

    private boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && y >= top && x < left + width && y < top + height;
    }

    private boolean overList(double x, double y) {
        return inside(x, y, DrawingTableLayout.listContentX(), DrawingTableLayout.listContentY(),
                DrawingTableLayout.listContentWidth(),
                DrawingTableLayout.listRows() * DrawingTableLayout.LIST_ROW_HEIGHT);
    }

    private boolean overSheet(double x, double y) {
        return inside(x, y, DrawingTableLayout.SHEET_X, DrawingTableLayout.SHEET_Y,
                DrawingTableLayout.SHEET_SIZE, DrawingTableLayout.SHEET_SIZE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = localX(mouseX);
        double y = localY(mouseY);

        if (button == 0 && startTracing(x, y)) {
            return true;
        }
        if (handleListClick(x, y, button) || handlePanelClick(x, y, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean startTracing(double x, double y) {
        TracingState state = menu.state();
        if (!overSheet(x, y) || (state != TracingState.READY && state != TracingState.DRAWING)) {
            return false;
        }
        drawing = true;
        ClientTracing.sample((int) (x - DrawingTableLayout.SHEET_X), (int) (y - DrawingTableLayout.SHEET_Y));
        return true;
    }

    private boolean handleListClick(double x, double y, int button) {
        if (button != 0 || !overList(x, y)) {
            return false;
        }
        int row = (int) ((y - DrawingTableLayout.listContentY()) / DrawingTableLayout.LIST_ROW_HEIGHT) + scrollIndex;
        if (row < 0 || row >= entries.size()) {
            return false;
        }
        String id = entries.get(row).id();
        if (listLocked()) {
            return true;
        }
        if (!id.equals(ClientTracing.blueprintId())) {
            NetworkHandler.sendToServer(new C2SSelectBlueprintPacket(id));
        }
        return true;
    }

    private boolean handlePanelClick(double x, double y, int button) {
        if (frameClicked(x, y, DrawingTableLayout.PAPER_FRAME_X)) {
            sendSlot(DrawingTableBlockEntity.SLOT_PAPER, button, Items.PAPER);
            return true;
        }
        if (frameClicked(x, y, DrawingTableLayout.INK_FRAME_X)) {
            sendSlot(DrawingTableBlockEntity.SLOT_INK, button, BlueprintItems.INK_BLOCK.get());
            return true;
        }
        if (frameClicked(x, y, DrawingTableLayout.RESULT_FRAME_X)) {
            if (menu.state() == TracingState.DONE) {
                NetworkHandler.sendToServer(new C2STakeResultFromTablePacket());
            }
            return true;
        }
        return false;
    }

    private boolean frameClicked(double x, double y, int frameX) {
        return inside(x, y, frameX, DrawingTableLayout.SLOT_FRAME_Y,
                DrawingTableLayout.FRAME_SIZE, DrawingTableLayout.FRAME_SIZE);
    }

    private void sendSlot(int slot, int button, Item item) {
        boolean empty = menu.getContainer().getItem(slot).isEmpty();
        if (button == 1 && empty && this.minecraft != null && this.minecraft.player != null
                && this.minecraft.player.getInventory().contains(new ItemStack(item))) {
            NetworkHandler.sendToServer(new C2SSetTableSlotPacket(slot, true));
        } else if (button == 0 && !empty) {
            NetworkHandler.sendToServer(new C2SSetTableSlotPacket(slot, false));
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (drawing && button == 0) {
            double x = localX(mouseX) - DrawingTableLayout.SHEET_X;
            double y = localY(mouseY) - DrawingTableLayout.SHEET_Y;
            ClientTracing.sample((int) x, (int) y);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (drawing && button == 0) {
            drawing = false;
            ClientTracing.lift();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    //? if forge {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    *///?} else {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double delta = scrollY;
    //?}
        if (overList(localX(mouseX), localY(mouseY))) {
            int maximum = Math.max(0, entries.size() - visibleRows());
            scrollIndex = Math.max(0, Math.min(maximum, scrollIndex - (int) Math.signum(delta)));
            return true;
        }
        //? if forge {
        /*return super.mouseScrolled(mouseX, mouseY, delta);
        *///?} else {
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        //?}
    }

    @Override
    public void removed() {
        super.removed();
        ClientTracing.clear();
    }
}
