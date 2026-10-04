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

    /** Whether this build may end before its last stage at all. */
    public boolean isExtendable() {
        BlueprintDefinition definition = definition();
        return definition != null && definition.isExtendable();
    }

    /** The section that has to stand before an extendable build may be ended, or null when it may end already. */
    @Nullable
    public String sectionNeededToEnd() {
        BlueprintDefinition definition = definition();
        if (definition == null || !definition.isExtendable() || stage() >= definition.min_stages) {
            return null;
        }
        BlueprintDefinition.StageSpec needed = definition.construction.get(definition.min_stages - 1);
        return needed == null ? null : needed.section;
    }

    /** Whether the build may be ended now, short of its last stage: it is extendable and has its minimum. */
    public boolean canEndHere() {
        BlueprintDefinition definition = definition();
        return definition != null && definition.isExtendable() && !complete() && stage() >= definition.min_stages;
    }

    /** Ends the build at the stage it has reached. The caller refunds any work on the current stage first. */
    public void endHere() {
        core.endHere();
    }

    /** Counts the first {@code stages} stages as built already. */
    public void skipBuilt(int stages) {
        core.advanceTo(stages);
    }

    /** What the stages built so far add up to, by each value they add to the result's data. */
    public Map<String, Integer> builtData() {
        BlueprintDefinition definition = definition();
        Map<String, Integer> data = new LinkedHashMap<>();
        if (definition == null || definition.construction == null) {
            return data;
        }
        int built = Math.min(stage(), definition.construction.size());
        for (int index = 0; index < built; index++) {
            BlueprintDefinition.StageSpec spec = definition.construction.get(index);
            if (spec != null && spec.adds != null) {
                spec.adds.forEach((key, value) -> data.merge(key, value, Integer::sum));
            }
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
