package me.mss1r.axiomata.construction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstructionProgressTest {
    private static final ConstructionPlan<String> PLAN = new ConstructionPlan<>(List.of(
            new ConstructionPlan.Stage<>("frame", List.of("timber"), 2),
            new ConstructionPlan.Stage<>("wheels", List.of("iron"), 1)
    ));

    @Test
    void advancesOnlyAfterTheRequiredWork() {
        ConstructionProgress progress = new ConstructionProgress();
        progress.begin("example:structure", "plain");
        progress.commitMaterials();

        assertFalse(progress.applyWork(PLAN));
        assertEquals(1, progress.workUnits());
        assertTrue(progress.materialsCommitted());

        assertTrue(progress.applyWork(PLAN));
        assertEquals(1, progress.stage());
        assertEquals(0, progress.workUnits());
        assertFalse(progress.materialsCommitted());
    }

    @Test
    void snapshotPreservesCompatibilityState() {
        ConstructionProgress original = new ConstructionProgress();
        original.begin("example:structure", "clean");
        original.commitMaterials();
        original.applyWork(PLAN);

        ConstructionProgress restored = new ConstructionProgress();
        restored.restore(original.snapshot());

        assertEquals(original.snapshot(), restored.snapshot());
    }

    @Test
    void rollbackReturnsTheStageThatBecameIncomplete() {
        ConstructionProgress progress = new ConstructionProgress();
        progress.begin("example:structure", "plain");
        progress.applyWork(PLAN);
        progress.applyWork(PLAN);

        assertEquals(0, progress.rollBackStage());
        assertEquals(0, progress.stage());
    }
}

