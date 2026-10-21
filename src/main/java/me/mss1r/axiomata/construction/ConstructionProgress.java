package me.mss1r.axiomata.construction;

import java.util.Objects;

public final class ConstructionProgress {
    private String planId = "";
    private String variantId = "";
    private int stage;
    private int workUnits;
    private boolean materialsCommitted;
    private boolean intrinsicallyFinished = true;

    public void begin(String planId, String variantId) {
        if (planId == null || planId.isBlank()) {
            throw new IllegalArgumentException("Plan id must not be blank");
        }
        this.planId = planId;
        this.variantId = variantId == null ? "" : variantId;
        stage = 0;
        workUnits = 0;
        materialsCommitted = false;
        intrinsicallyFinished = false;
    }

    public void finishWithoutPlan() {
        planId = "";
        variantId = "";
        stage = 0;
        workUnits = 0;
        materialsCommitted = false;
        intrinsicallyFinished = true;
    }

    public String planId() {
        return planId;
    }

    public String variantId() {
        return variantId;
    }

    public int stage() {
        return stage;
    }

    public int workUnits() {
        return workUnits;
    }

    public boolean materialsCommitted() {
        return materialsCommitted;
    }

    public void commitMaterials() {
        materialsCommitted = true;
    }

    public boolean hasCurrentStageWork() {
        return workUnits > 0 || materialsCommitted;
    }

    public void cancelCurrentStage() {
        workUnits = 0;
        materialsCommitted = false;
    }

    public boolean complete(ConstructionPlan<?> plan) {
        Objects.requireNonNull(plan, "plan");
        return intrinsicallyFinished || stage >= plan.stageCount();
    }

    public <M> ConstructionPlan.Stage<M> currentStage(ConstructionPlan<M> plan) {
        return complete(plan) ? null : plan.stage(stage);
    }

    public boolean applyWork(ConstructionPlan<?> plan) {
        ConstructionPlan.Stage<?> current = currentStage(plan);
        if (current == null) {
            return false;
        }
        workUnits++;
        if (workUnits < current.workUnits()) {
            return false;
        }
        workUnits = 0;
        stage++;
        materialsCommitted = false;
        return true;
    }

    public int rollBackStage() {
        if (stage <= 0) {
            cancelCurrentStage();
            return -1;
        }
        cancelCurrentStage();
        stage--;
        intrinsicallyFinished = false;
        return stage;
    }

    public Snapshot snapshot() {
        return new Snapshot(planId, variantId, stage, workUnits, materialsCommitted,
                intrinsicallyFinished);
    }

    public void restore(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        planId = snapshot.planId();
        variantId = snapshot.variantId();
        stage = Math.max(0, snapshot.stage());
        workUnits = Math.max(0, snapshot.workUnits());
        materialsCommitted = snapshot.materialsCommitted();
        intrinsicallyFinished = snapshot.intrinsicallyFinished();
    }

    public record Snapshot(String planId, String variantId, int stage, int workUnits,
                           boolean materialsCommitted, boolean intrinsicallyFinished) {
        public Snapshot {
            planId = planId == null ? "" : planId;
            variantId = variantId == null ? "" : variantId;
        }
    }
}
