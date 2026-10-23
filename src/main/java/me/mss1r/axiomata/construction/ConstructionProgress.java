package me.mss1r.axiomata.construction;

import java.util.Objects;

public final class ConstructionProgress {
    private String planId = "";
    private String variantId = "";
    private int stage;
    private int furthestStage;
    private double workUnits;
    private boolean materialsCommitted;
    private boolean intrinsicallyFinished = true;

    public void begin(String planId, String variantId) {
        if (planId == null || planId.isBlank()) {
            throw new IllegalArgumentException("Plan id must not be blank");
        }
        this.planId = planId;
        this.variantId = variantId == null ? "" : variantId;
        stage = 0;
        furthestStage = 0;
        workUnits = 0;
        materialsCommitted = false;
        intrinsicallyFinished = false;
    }

    public void finishWithoutPlan() {
        planId = "";
        variantId = "";
        stage = 0;
        furthestStage = 0;
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

    /** Furthest completed boundary, retained when stages are taken apart. */
    public int furthestStage() {
        return furthestStage;
    }

    public int workUnits() {
        return (int) workUnits;
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
        return applyWork(plan, 1);
    }

    /** Work stops at the stage boundary: the next stage must pay for its own materials. */
    public boolean applyWork(ConstructionPlan<?> plan, double amount) {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Work must be finite and positive");
        }
        ConstructionPlan.Stage<?> current = currentStage(plan);
        if (current == null) {
            return false;
        }
        workUnits += amount;
        if (workUnits + 1.0E-9 < current.workUnits()) {
            return false;
        }
        workUnits = 0;
        stage++;
        furthestStage = Math.max(furthestStage, stage);
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
                intrinsicallyFinished, furthestStage);
    }

    public void restore(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        planId = snapshot.planId();
        variantId = snapshot.variantId();
        stage = Math.max(0, snapshot.stage());
        furthestStage = Math.max(stage, snapshot.furthestStage());
        workUnits = Double.isFinite(snapshot.workUnits()) ? Math.max(0, snapshot.workUnits()) : 0;
        materialsCommitted = snapshot.materialsCommitted();
        intrinsicallyFinished = snapshot.intrinsicallyFinished();
    }

    public record Snapshot(String planId, String variantId, int stage, double workUnits,
                           boolean materialsCommitted, boolean intrinsicallyFinished, int furthestStage) {
        public Snapshot {
            planId = planId == null ? "" : planId;
            variantId = variantId == null ? "" : variantId;
        }
    }
}
