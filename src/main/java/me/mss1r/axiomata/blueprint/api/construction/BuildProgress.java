package me.mss1r.axiomata.blueprint.api.construction;

import me.mss1r.axiomata.construction.ConstructionProgress;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BuildProgress {
    private static final String BLUEPRINT_TAG = "Blueprint";
    private static final String STAGE_TAG = "BuildStage";
    private static final String HITS_TAG = "BuildHits";
    private static final String QUALITY_TAG = "BuildQuality";
    private static final String MATERIALS_COMMITTED_TAG = "BuildMaterialsCommitted";
    private static final String ENDED_TAG = "BuildEnded";
    private static final String PAID_TAG = "BuildPaid";

    private final ConstructionProgress core = new ConstructionProgress();
    // Items taken for the current stage, so that cancelling it gives back the same items rather than a tag's first.
    private final List<ItemStack> paid = new ArrayList<>();

    public static BuildProgress finished() {
        return new BuildProgress();
    }

    public void begin(String blueprintId, BuildQuality quality) {
        if (blueprintId == null || blueprintId.isBlank()) {
            core.finishWithoutPlan();
            return;
        }
        BuildQuality resolvedQuality = quality == null ? BuildQuality.PLAIN : quality;
        core.begin(blueprintId, resolvedQuality.id());
        paid.clear();
    }

    public String blueprintId() {
        return core.planId();
    }

    public int stage() {
        return core.stage();
    }

    public int hits() {
        return core.workUnits();
    }

    public boolean materialsCommitted() {
        return core.materialsCommitted();
    }

    /** Marks the current stage's materials as taken without recording which items, as for creative players. */
    public void commitMaterials() {
        commitMaterials(List.of());
    }

    /** Marks the current stage's materials as taken, recording the items that were taken for them. */
    public void commitMaterials(List<ItemStack> taken) {
        core.commitMaterials();
        paid.clear();
        taken.forEach(stack -> paid.add(stack.copy()));
    }

    /**
     * Items to give back for the current stage's taken materials: the items that were taken, or, for a stage taken
     * before they were recorded, each material's {@link BlueprintDefinition.Material#returnStack()}.
     */
    public List<ItemStack> currentStageRefund() {
        if (!paid.isEmpty()) {
            return paid.stream().map(ItemStack::copy).toList();
        }
        BlueprintConstructionPlan.Stage stage = currentStage();
        return stage == null ? List.of()
                : stage.materials().stream().map(BlueprintDefinition.Material::returnStack).toList();
    }

    public boolean hasCurrentStageWork() {
        return core.hasCurrentStageWork();
    }

    public void cancelCurrentStage() {
        core.cancelCurrentStage();
        paid.clear();
    }

    public BlueprintConstructionPlan plan() {
        return BlueprintConstructionPlan.of(BlueprintDefinitions.get(blueprintId()), quality());
    }

    public boolean complete() {
        BlueprintConstructionPlan plan = plan();
        return core.complete(plan.core());
    }

    public BlueprintConstructionPlan.Stage currentStage() {
        BlueprintConstructionPlan plan = plan();
        return core.complete(plan.core()) ? null : plan.stage(stage());
    }

    public int builtStages() {
        return complete() ? Integer.MAX_VALUE : stage();
    }

    public boolean isExtendable() {
        BlueprintDefinition definition = definition();
        return definition != null && definition.isExtendable();
    }

    /** Section that must be built before the build can be ended early, or null if it can be ended now. */
    @Nullable
    public String sectionNeededToEnd() {
        BlueprintDefinition definition = definition();
        if (definition == null || !definition.isExtendable() || stage() >= definition.minStages()) {
            return null;
        }
        return definition.stages().get(definition.minStages() - 1).section();
    }

    /** True if the build is extendable, not complete, and has at least {@code minStages} stages built. */
    public boolean canEndHere() {
        BlueprintDefinition definition = definition();
        return definition != null && definition.isExtendable() && !complete() && stage() >= definition.minStages();
    }

    /**
     * Marks the build finished at the current stage. The caller must refund materials committed to that stage first.
     */
    public void endHere() {
        core.endHere();
        paid.clear();
    }

    /** Marks the first {@code stages} stages as built without doing their work. */
    public void skipBuilt(int stages) {
        core.advanceTo(stages);
        paid.clear();
    }

    /** Sum of the {@code adds} values of all built stages. */
    public Map<String, Integer> builtData() {
        BlueprintDefinition definition = definition();
        Map<String, Integer> data = new LinkedHashMap<>();
        if (definition == null) {
            return data;
        }
        int built = Math.min(stage(), definition.stageCount());
        for (int index = 0; index < built; index++) {
            definition.stages().get(index).adds().forEach((key, value) -> data.merge(key, value, Integer::sum));
        }
        return data;
    }

    @Nullable
    private BlueprintDefinition definition() {
        return BlueprintDefinitions.get(blueprintId());
    }
    /** Returns true only when this hit completed the current stage. */
    public boolean strike() {
        BlueprintConstructionPlan plan = plan();
        boolean advanced = core.applyWork(plan.core());
        if (advanced) {
            paid.clear();
        }
        return advanced;
    }

    public int dismantle() {
        // The core returns the stage that became incomplete so its materials can be refunded.
        paid.clear();
        return core.rollBackStage();
    }

    public void save(CompoundTag tag) {
        ConstructionProgress.Snapshot snapshot = core.snapshot();
        tag.putString(BLUEPRINT_TAG, snapshot.planId());
        tag.putInt(STAGE_TAG, snapshot.stage());
        tag.putInt(HITS_TAG, snapshot.workUnits());
        tag.putString(QUALITY_TAG, snapshot.variantId());
        tag.putBoolean(MATERIALS_COMMITTED_TAG, snapshot.materialsCommitted());
        tag.putBoolean(ENDED_TAG, snapshot.intrinsicallyFinished() && !snapshot.planId().isEmpty());
        // Item and count only: a stage takes plain items, and this needs no registry access to save.
        ListTag paidTag = new ListTag();
        for (ItemStack stack : paid) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            entry.putInt("count", stack.getCount());
            paidTag.add(entry);
        }
        tag.put(PAID_TAG, paidTag);
    }

    public void load(CompoundTag tag) {
        String blueprintId = tag.getString(BLUEPRINT_TAG);
        BuildQuality quality = BuildQuality.byId(tag.getString(QUALITY_TAG));
        core.restore(new ConstructionProgress.Snapshot(
                blueprintId,
                quality.id(),
                tag.getInt(STAGE_TAG),
                tag.getInt(HITS_TAG),
                tag.getBoolean(MATERIALS_COMMITTED_TAG),
                blueprintId.isEmpty() || tag.getBoolean(ENDED_TAG)));
        paid.clear();
        for (Tag value : tag.getList(PAID_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("id"));
            if (id != null && BuiltInRegistries.ITEM.containsKey(id) && entry.getInt("count") > 0) {
                paid.add(new ItemStack(BuiltInRegistries.ITEM.get(id), entry.getInt("count")));
            }
        }
    }

    private BuildQuality quality() {
        return BuildQuality.byId(core.variantId());
    }
}
