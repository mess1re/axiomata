package me.mss1r.axiomata.blueprint.blockentity;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import me.mss1r.axiomata.blueprint.tracing.BlueprintOutline;
import me.mss1r.axiomata.blueprint.tracing.OutlineCatalog;
import me.mss1r.axiomata.blueprint.tracing.TracingRules;
import me.mss1r.axiomata.blueprint.tracing.TracingSession;
import me.mss1r.axiomata.blueprint.tracing.TracingState;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.menu.DrawingTableMenu;
import me.mss1r.axiomata.blueprint.registry.BlueprintBlockEntities;
import me.mss1r.axiomata.blueprint.registry.BlueprintItems;
import net.minecraft.core.BlockPos;
//? if neoforge {
import net.minecraft.core.HolderLookup;
//?}
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// Tracing state belongs to the table, not its menu. Closing the screen must not discard a
// half-finished sheet.
public class DrawingTableBlockEntity extends BlockEntity implements ExtendedMenuProvider {
    public static final int SLOT_PAPER = 0;
    public static final int SLOT_INK = 1;
    public static final int SLOT_RESULT = 2;
    public static final int SLOT_COUNT = 3;

    public static final int DATA_COVERAGE = 0;
    public static final int DATA_WANDERED = 1;
    public static final int DATA_WANDER_BUDGET = 2;
    public static final int DATA_STATE = 3;
    public static final int DATA_COUNT = 4;

    private static final String ITEMS_TAG = "Items";
    private static final String BLUEPRINT_TAG = "Blueprint";
    private static final String COVERED_TAG = "Covered";
    private static final String WANDERED_TAG = "Wandered";
    private static final String RUINED_TAG = "Ruined";

    private final SimpleContainer container = new SimpleContainer(SLOT_COUNT);

    private String blueprintId = "";
    private TracingSession session;
    private boolean ruined;
    private long sheetRevision;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_COVERAGE -> Math.round(coverage() * 1000.0F);
                case DATA_WANDERED -> session == null ? 0 : session.wandered();
                case DATA_WANDER_BUDGET -> session == null ? 0 : session.wanderBudget();
                case DATA_STATE -> state().ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public DrawingTableBlockEntity(BlockPos pos, BlockState state) {
        super(BlueprintBlockEntities.DRAWING_TABLE.get(), pos, state);
        container.addListener(changed -> setChanged());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.axiomata.drawing_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new DrawingTableMenu(id, inv, this, data);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(getBlockPos());
    }

    public SimpleContainer getContainer() {
        return container;
    }

    public ContainerData getData() {
        return data;
    }
    public String blueprintId() {
        return blueprintId;
    }

    public TracingSession session() {
        return session;
    }

    public long sheetRevision() {
        return sheetRevision;
    }

    public BlueprintOutline outline() {
        return blueprintId.isEmpty() ? null : OutlineCatalog.get(outlineId(blueprintId));
    }

    /** Outline id for a blueprint, which may point at another blueprint's outline. */
    private static String outlineId(String blueprintId) {
        var definition = BlueprintDefinitions.get(blueprintId);
        return definition == null ? blueprintId : definition.outlineId(blueprintId);
    }

    public float coverage() {
        return session == null ? 0.0F : session.coverage();
    }

    public TracingState state() {
        if (ruined) {
            return TracingState.RUINED;
        }
        if (blueprintId.isEmpty() || BlueprintDefinitions.get(blueprintId) == null || outline() == null) {
            return TracingState.IDLE;
        }
        if (!hasPaper() || !hasInk()) {
            return TracingState.IDLE;
        }
        if (session == null || session.coverage() <= 0.0F) {
            return TracingState.READY;
        }
        return session.complete() ? TracingState.DONE : TracingState.DRAWING;
    }

    public boolean hasWorkInProgress() {
        return session != null && session.coverage() > 0.0F;
    }
    public boolean select(String id) {
        if (id == null || BlueprintDefinitions.get(id) == null || OutlineCatalog.get(outlineId(id)) == null) {
            return false;
        }
        // Changing the selection after the first stroke would silently throw work away. Taking
        // the paper out is the explicit reset path.
        if (hasWorkInProgress() && !id.equals(blueprintId)) {
            return false;
        }
        if (id.equals(blueprintId)) {
            return true;
        }
        blueprintId = id;
        session = null;
        ruined = false;
        sheetRevision++;
        setChanged();
        return true;
    }

    public boolean trace(int[] points, int count) {
        TracingState current = state();
        if (current != TracingState.READY && current != TracingState.DRAWING) {
            return false;
        }
        BlueprintOutline outline = outline();
        if (outline == null) {
            return false;
        }
        if (session == null) {
            // The client and server quantize pixels independently, hence the extra tolerance here.
            session = outline.openSession(TracingRules.SERVER_TOLERANCE_PIXELS);
        }
        boolean drawn = false;
        for (int index = 0; index < count; index++) {
            drawn |= session.apply(points[index * 2], points[index * 2 + 1]);
        }
        if (session.ruined()) {
            spoilSheet();
        }
        setChanged();
        return drawn;
    }

    public void liftPen() {
        if (session != null) {
            session.lift();
        }
    }

    private void spoilSheet() {
        ruined = true;
        session = null;
        sheetRevision++;
        container.setItem(SLOT_PAPER, ItemStack.EMPTY);
        container.setItem(SLOT_RESULT, ItemStack.EMPTY);
    }
    public void notePaperChanged() {
        if (ruined && hasPaper()) {
            ruined = false;
            sheetRevision++;
            setChanged();
        }
        // Removing the sheet is how the player abandons the current drawing.
        if (!hasPaper() && session != null) {
            session = null;
            sheetRevision++;
            setChanged();
        }
    }

    public void clearSheet() {
        session = null;
        ruined = false;
        sheetRevision++;
        setChanged();
    }

    public boolean hasPaper() {
        return container.getItem(SLOT_PAPER).is(Items.PAPER);
    }

    public boolean hasInk() {
        return container.getItem(SLOT_INK).getItem() == BlueprintItems.INK_BLOCK.get();
    }
    //? if forge {
    /*@Override
    public void load(CompoundTag tag) {
        super.load(tag);
        loadBlueprintData(tag, null);
    }
    *///?} else {
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadBlueprintData(tag, registries);
    }
    //?}

    private void loadBlueprintData(CompoundTag tag,
            //? if forge {
            /*Object registries
            *///?} else {
            HolderLookup.Provider registries
            //?}
    ) {
        container.clearContent();
        ListTag listTag = tag.getList(ITEMS_TAG, 10);
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag stackTag = listTag.getCompound(i);
            int slot = stackTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < container.getContainerSize()) {
                //? if forge {
                /*container.setItem(slot, ItemStack.of(stackTag));
                *///?} else {
                container.setItem(slot, ItemStack.parse(registries, stackTag).orElse(ItemStack.EMPTY));
                //?}
            }
        }

        blueprintId = tag.getString(BLUEPRINT_TAG);
        ruined = tag.getBoolean(RUINED_TAG);
        session = null;
        BlueprintOutline outline = outline();
        if (outline != null && tag.contains(COVERED_TAG)) {
            TracingSession restored = outline.openSession(TracingRules.SERVER_TOLERANCE_PIXELS);
            restored.restore(tag.getByteArray(COVERED_TAG), tag.getInt(WANDERED_TAG));
            session = restored;
        }
    }

    //? if forge {
    /*@Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveBlueprintData(tag, null);
    }
    *///?} else {
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveBlueprintData(tag, registries);
    }
    //?}

    private void saveBlueprintData(CompoundTag tag,
            //? if forge {
            /*Object registries
            *///?} else {
            HolderLookup.Provider registries
            //?}
    ) {
        ListTag listTag = new ListTag();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                //? if forge {
                /*CompoundTag stackTag = stack.save(new CompoundTag());
                *///?} else {
                CompoundTag stackTag = (CompoundTag) stack.save(registries, new CompoundTag());
                //?}
                stackTag.putByte("Slot", (byte) slot);
                listTag.add(stackTag);
            }
        }
        tag.put(ITEMS_TAG, listTag);

        tag.putString(BLUEPRINT_TAG, blueprintId);
        tag.putBoolean(RUINED_TAG, ruined);
        if (session != null) {
            tag.putByteArray(COVERED_TAG, session.snapshot());
            tag.putInt(WANDERED_TAG, session.wandered());
        }
    }
}
