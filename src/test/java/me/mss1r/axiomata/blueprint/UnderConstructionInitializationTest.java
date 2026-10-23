package me.mss1r.axiomata.blueprint;

import me.mss1r.axiomata.blueprint.api.construction.BuildProgress;
import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UnderConstructionInitializationTest {
    private UnderConstruction machine() {
        Entity entity = mock(Entity.class, withSettings().extraInterfaces(UnderConstruction.class));
        UnderConstruction machine = (UnderConstruction) entity;
        doCallRealMethod().when(machine).isFullyBuilt();
        return machine;
    }

    @Test void constructorProbeDoesNotReadUninitializedProgress() {
        UnderConstruction machine = machine();
        assertTrue(machine.isFullyBuilt());
        verify(machine, never()).buildProgress();
    }

    @Test void serverUsesRealProgressOnceInitialized() {
        UnderConstruction machine = machine();
        when(((Entity) machine).level()).thenReturn(mock(Level.class));
        BuildProgress progress = mock(BuildProgress.class);
        when(machine.buildProgress()).thenReturn(progress);
        when(progress.complete()).thenReturn(false);
        assertFalse(machine.isFullyBuilt());
        when(progress.complete()).thenReturn(true);
        assertTrue(machine.isFullyBuilt());
    }

    @Test void absentProgressDuringInitializationIsSafe() {
        UnderConstruction machine = machine();
        when(((Entity) machine).level()).thenReturn(mock(Level.class));
        assertTrue(machine.isFullyBuilt());
    }
}
