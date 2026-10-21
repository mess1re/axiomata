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
    private BlueprintConstructionPlan cachedPlan;
    private BlueprintDefinition planDefinition;
    private BuildQuality planQuality;

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
        // Asked on every collision query of an unfinished build; rebuilt only when the definition or quality changes.
        BlueprintDefinition definition = BlueprintDefinitions.get(blueprintId());
        BuildQuality quality = quality();
        if (cachedPlan == null || planDefinition != definition || planQuality != quality) {
            cachedPlan = BlueprintConstructionPlan.of(definition, quality);
            planDefinition = definition;
            planQuality = quality;
        }
        return cachedPlan;
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

    /** Share of the build's hammer work done, from 0 for a bare site to 1 once every stage stands. */
    public float workDone() {
        BlueprintConstructionPlan plan = plan();
        int total = plan.totalHits();
        if (total <= 0 || core.complete(plan.core())) {
            return 1.0F;
        }
        int done = hits();
        for (int stage = 0; stage < Math.min(stage(), plan.stageCount()); stage++) {
            done += plan.stage(stage).hits();
        }
        return Math.min(1.0F, done / (float) total);
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
