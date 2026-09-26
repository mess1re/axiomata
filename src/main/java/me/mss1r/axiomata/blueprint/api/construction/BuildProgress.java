package me.mss1r.axiomata.blueprint.api.construction;

import me.mss1r.axiomata.construction.ConstructionProgress;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import net.minecraft.nbt.CompoundTag;

public final class BuildProgress {
    private static final String BLUEPRINT_TAG = "Blueprint";
    private static final String STAGE_TAG = "BuildStage";
    private static final String HITS_TAG = "BuildHits";
    private static final String QUALITY_TAG = "BuildQuality";
    private static final String MATERIALS_COMMITTED_TAG = "BuildMaterialsCommitted";

    private final ConstructionProgress core = new ConstructionProgress();

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

    public void commitMaterials() {
        core.commitMaterials();
    }

    public boolean hasCurrentStageWork() {
        return core.hasCurrentStageWork();
    }

    public void cancelCurrentStage() {
        core.cancelCurrentStage();
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
    /** Returns true only when this hit completed the current stage. */
    public boolean strike() {
        BlueprintConstructionPlan plan = plan();
        return core.applyWork(plan.core());
    }

    public int dismantle() {
        // The core returns the stage that became incomplete so its materials can be refunded.
        return core.rollBackStage();
    }

    public void save(CompoundTag tag) {
        ConstructionProgress.Snapshot snapshot = core.snapshot();
        tag.putString(BLUEPRINT_TAG, snapshot.planId());
        tag.putInt(STAGE_TAG, snapshot.stage());
        tag.putInt(HITS_TAG, snapshot.workUnits());
        tag.putString(QUALITY_TAG, snapshot.variantId());
        tag.putBoolean(MATERIALS_COMMITTED_TAG, snapshot.materialsCommitted());
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
                blueprintId.isEmpty()));
    }

    private BuildQuality quality() {
        return BuildQuality.byId(core.variantId());
    }
}
