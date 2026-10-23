package me.mss1r.axiomata.construction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConstructionProgressTest {
    private static final ConstructionPlan<String> PLAN = new ConstructionPlan<>(List.of(
            new ConstructionPlan.Stage<>("frame", List.of("timber"), 2),
            new ConstructionPlan.Stage<>("wheels", List.of("iron"), 1)
    ));

    @Test
    void thirtyPercentLessWorkTakesSevenHitsInsteadOfTen() {
        ConstructionPlan<String> plan = new ConstructionPlan<>(List.of(
                new ConstructionPlan.Stage<>("hull", List.of("timber"), 10)));
        ConstructionProgress progress = new ConstructionProgress();
        progress.begin("example:ship", "plain");
        progress.commitMaterials();
        for (int hit = 0; hit < 6; hit++) {
            assertFalse(progress.applyWork(plan, 1 / .7));
            assertTrue(progress.materialsCommitted());
        }
        assertTrue(progress.applyWork(plan, 1 / .7));
        assertTrue(progress.complete(plan));
    }

    @Test
    void fractionalWorkSurvivesReloadAndDoesNotSpillIntoTheNextStage() {
        ConstructionProgress progress = new ConstructionProgress();
        progress.begin("example:ship", "plain");
        progress.applyWork(PLAN, 1 / .7);
        ConstructionProgress restored = new ConstructionProgress();
        restored.restore(progress.snapshot());
        assertEquals(progress.snapshot(), restored.snapshot());
        assertTrue(restored.applyWork(PLAN, 1 / .7));
        assertEquals(1, restored.stage());
        assertEquals(0, restored.workUnits());
        assertFalse(restored.materialsCommitted());
        assertFalse(restored.complete(PLAN));
    }

    @Test
    void cancellingDiscardsFractionalWorkAndInvalidWorkIsRejected() {
        ConstructionProgress progress = new ConstructionProgress();
        progress.begin("example:ship", "plain");
        progress.applyWork(PLAN, .5);
        assertTrue(progress.hasCurrentStageWork());
        progress.cancelCurrentStage();
        assertEquals(0, progress.snapshot().workUnits());
        assertThrows(IllegalArgumentException.class, () -> progress.applyWork(PLAN, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> progress.applyWork(PLAN, -1));
    }

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

    @Test
    void aBuildEndedEarlyInAnOldSaveStaysFinishedUntilTakenApart() {
        ConstructionProgress progress = new ConstructionProgress();
        progress.restore(new ConstructionProgress.Snapshot("example:structure", "plain", 1, 0, false, true));
        assertTrue(progress.complete(PLAN));

        assertEquals(0, progress.rollBackStage());
        assertFalse(progress.complete(PLAN));
    }
}
