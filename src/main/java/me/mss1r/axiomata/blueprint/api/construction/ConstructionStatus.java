package me.mss1r.axiomata.blueprint.api.construction;

/**
 * What every side knows of a build: its blueprint, how many stages stand and the work done on the next. The server
 * reads it from {@link BuildProgress}; clients receive it from Axiomata whenever it changes.
 */
public record ConstructionStatus(String blueprintId, int builtStages, int hits, int stageHits, boolean complete) {
    /** A result that was never under construction, or whose state a client has not received yet. */
    public static final ConstructionStatus FINISHED = new ConstructionStatus("", 0, 0, 0, true);

    public ConstructionStatus {
        blueprintId = blueprintId == null ? "" : blueprintId;
    }

    public static ConstructionStatus of(BuildProgress progress) {
        BlueprintConstructionPlan.Stage stage = progress.currentStage();
        return new ConstructionStatus(progress.blueprintId(), progress.stage(), progress.hits(),
                stage == null ? 0 : stage.hits(), stage == null);
    }
}
