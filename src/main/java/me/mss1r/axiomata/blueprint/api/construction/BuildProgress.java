package me.mss1r.axiomata.blueprint.api.construction;

import me.mss1r.axiomata.construction.ConstructionProgress;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BuildProgress {
    private static final String BLUEPRINT_TAG = "Blueprint";
    private static final String STAGE_TAG = "BuildStage";
    private static final String HITS_TAG = "BuildHits";
    private static final String QUALITY_TAG = "BuildQuality";
    private static final String MATERIALS_COMMITTED_TAG = "BuildMaterialsCommitted";
    private static final String ENDED_TAG = "BuildEnded";

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
    }

    /** Marks the first {@code stages} stages as built without doing their work. */
    public void skipBuilt(int stages) {
        core.advanceTo(stages);
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
        tag.putBoolean(ENDED_TAG, snapshot.intrinsicallyFinished() && !snapshot.planId().isEmpty());
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
    }

    private BuildQuality quality() {
        return BuildQuality.byId(core.variantId());
    }
}
