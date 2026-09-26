package me.mss1r.axiomata.blueprint.menu;

import me.mss1r.axiomata.blueprint.blockentity.DrawingTableBlockEntity;
import me.mss1r.axiomata.blueprint.tracing.TracingRules;
import me.mss1r.axiomata.blueprint.tracing.TracingSession;
import me.mss1r.axiomata.blueprint.tracing.TracingState;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.api.event.BlueprintCreatedEvent;
import me.mss1r.axiomata.blueprint.api.event.BlueprintCreationCheckEvent;
import me.mss1r.axiomata.blueprint.api.event.BlueprintEvents;
import me.mss1r.axiomata.blueprint.item.BlueprintItem;
import me.mss1r.axiomata.blueprint.registry.BlueprintItems;
import me.mss1r.axiomata.blueprint.registry.BlueprintMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

public class DrawingTableMenu extends AbstractContainerMenu {
    private final SimpleContainer container;
    private final ContainerData data;
    private final net.minecraft.world.level.Level level;
    private final BlockPos tablePos;

    private long strokeTick = Long.MIN_VALUE;
    private int strokeSamples;

    public DrawingTableMenu(int id, Inventory playerInv, DrawingTableBlockEntity table, ContainerData data) {
        this(id, playerInv, table.getContainer(), data, table.getBlockPos());
    }

    public DrawingTableMenu(int id, Inventory playerInv, BlockPos pos) {
        this(id, playerInv, containerAt(playerInv, pos),
                new net.minecraft.world.inventory.SimpleContainerData(DrawingTableBlockEntity.DATA_COUNT), pos);
    }

    private DrawingTableMenu(int id, Inventory playerInv, SimpleContainer container, ContainerData data, BlockPos pos) {
        super(BlueprintMenus.DRAWING_TABLE_MENU.get(), id);
        this.container = container;
        this.data = data;
        this.level = playerInv.player.level();
        this.tablePos = pos;

        addSlot(new Slot(container, DrawingTableBlockEntity.SLOT_PAPER,
                DrawingTableLayout.slotX(DrawingTableLayout.PAPER_FRAME_X), DrawingTableLayout.slotY()) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.PAPER);
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });

        addSlot(new Slot(container, DrawingTableBlockEntity.SLOT_INK,
                DrawingTableLayout.slotX(DrawingTableLayout.INK_FRAME_X), DrawingTableLayout.slotY()) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == BlueprintItems.INK_BLOCK.get();
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });

        addSlot(new Slot(container, DrawingTableBlockEntity.SLOT_RESULT,
                DrawingTableLayout.slotX(DrawingTableLayout.RESULT_FRAME_X), DrawingTableLayout.slotY()) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });

        addDataSlots(data);
    }

    private static SimpleContainer containerAt(Inventory playerInv, BlockPos pos) {
        BlockEntity blockEntity = playerInv.player.level().getBlockEntity(pos);
        if (blockEntity instanceof DrawingTableBlockEntity table) {
            return table.getContainer();
        }
        return new SimpleContainer(DrawingTableBlockEntity.SLOT_COUNT);
    }

    public SimpleContainer getContainer() {
        return container;
    }

    public DrawingTableBlockEntity table() {
        return level.getBlockEntity(tablePos) instanceof DrawingTableBlockEntity table ? table : null;
    }
    public TracingState state() {
        return TracingState.byOrdinal(data.get(DrawingTableBlockEntity.DATA_STATE));
    }

    public float coverage() {
        return data.get(DrawingTableBlockEntity.DATA_COVERAGE) / 1000.0F;
    }

    public int wandered() {
        return data.get(DrawingTableBlockEntity.DATA_WANDERED);
    }

    public int wanderBudget() {
        return data.get(DrawingTableBlockEntity.DATA_WANDER_BUDGET);
    }
    public boolean allowStroke(long gameTime, int samples) {
        // Several packets may arrive in one tick. Limit the total, not each packet separately.
        if (gameTime != strokeTick) {
            strokeTick = gameTime;
            strokeSamples = 0;
        }
        if (strokeSamples + samples > TracingRules.MAX_SAMPLES_PER_TICK) {
            return false;
        }
        strokeSamples += samples;
        return true;
    }

    @Override
    public void slotsChanged(net.minecraft.world.Container changed) {
        refreshResult();
        super.slotsChanged(changed);
    }

    public void refreshResult() {
        DrawingTableBlockEntity table = table();
        if (table == null) {
            return;
        }
        table.notePaperChanged();
        ItemStack shown = ItemStack.EMPTY;
        if (table.state() == TracingState.DONE) {
            BlueprintDefinition recipe = BlueprintDefinitions.get(table.blueprintId());
            if (recipe != null) {
                shown = BlueprintItem.create(recipe, table.blueprintId());
            }
        }
        container.setItem(DrawingTableBlockEntity.SLOT_RESULT, shown);
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level() != this.level) {
            return false;
        }
        if (!(level.getBlockEntity(tablePos) instanceof DrawingTableBlockEntity)) {
            return false;
        }
        double dx = player.getX() - (tablePos.getX() + 0.5);
        double dy = player.getY() - (tablePos.getY() + 0.5);
        double dz = player.getZ() - (tablePos.getZ() + 0.5);
        return (dx * dx + dy * dy + dz * dz) <= 64.0;
    }

    public ItemStack takeResult(Player player) {
        DrawingTableBlockEntity table = table();
        if (table == null || table.state() != TracingState.DONE) {
            return ItemStack.EMPTY;
        }
        BlueprintDefinition recipe = BlueprintDefinitions.get(table.blueprintId());
        if (recipe == null) {
            return ItemStack.EMPTY;
        }

        ItemStack paper = container.getItem(DrawingTableBlockEntity.SLOT_PAPER);
        ItemStack ink = container.getItem(DrawingTableBlockEntity.SLOT_INK);
        if (paper.isEmpty() || ink.isEmpty() || ink.getItem() != BlueprintItems.INK_BLOCK.get()) {
            return ItemStack.EMPTY;
        }

        ItemStack out = BlueprintItem.create(recipe, table.blueprintId());
        if (out.isEmpty()) {
            return ItemStack.EMPTY;
        }
        TracingSession session = table.session();
        BlueprintItem.stamp(out, player.getGameProfile().getName(),
                session == null ? 0 : session.wandered(),
                session == null ? 0 : session.wanderBudget());
        if (player instanceof ServerPlayer serverPlayer) {
            BlueprintCreationCheckEvent check = new BlueprintCreationCheckEvent(serverPlayer, out);
            BlueprintEvents.CREATION_CHECK.invoker().check(check);
            if (!check.allowed()) {
                return ItemStack.EMPTY;
            }
        }

        paper.shrink(1);
        ink.shrink(1);
        if (paper.getCount() <= 0) {
            container.setItem(DrawingTableBlockEntity.SLOT_PAPER, ItemStack.EMPTY);
        }
        if (ink.getCount() <= 0) {
            container.setItem(DrawingTableBlockEntity.SLOT_INK, new ItemStack(BlueprintItems.INKWELL.get()));
        }

        table.clearSheet();
        container.setItem(DrawingTableBlockEntity.SLOT_RESULT, ItemStack.EMPTY);
        broadcastChanges();

        if (player instanceof ServerPlayer serverPlayer) {
            BlueprintEvents.CREATED.invoker().created(new BlueprintCreatedEvent(serverPlayer, out));
        }
        return out;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == DrawingTableBlockEntity.SLOT_RESULT) {
            ItemStack produced = takeResult(player);
            if (produced.isEmpty()) {
                return ItemStack.EMPTY;
            }
            if (!player.addItem(produced) && !produced.isEmpty()) {
                player.drop(produced, false);
            }
            return produced;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        returnToPlayer(player, DrawingTableBlockEntity.SLOT_PAPER);
        returnToPlayer(player, DrawingTableBlockEntity.SLOT_INK);
    }

    private void returnToPlayer(Player player, int slot) {
        DrawingTableBlockEntity table = table();
        // Paper and ink stay in the table while a drawing exists. Returning either on close would
        // make reopening look as if the saved work had vanished.
        if (table != null && table.hasWorkInProgress()) {
            return;
        }
        ItemStack stack = container.getItem(slot);
        if (!stack.isEmpty()) {
            player.getInventory().placeItemBackInInventory(stack);
            container.setItem(slot, ItemStack.EMPTY);
        }
    }
}
