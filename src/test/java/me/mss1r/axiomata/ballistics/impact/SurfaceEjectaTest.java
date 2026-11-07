package me.mss1r.axiomata.ballistics.impact;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class SurfaceEjectaTest {
    @Test void surfaceLayerDoesNotReuseTheBulkMass() {
        var material = new Fragmentation.Material(650, 1000, .04, .2);
        var split = SurfaceEjecta.release(1, 100_000, material);
        assertEquals(26, split.fine().totalMassKg(), 1e-9);
        assertEquals(624, split.coarseMassKg(), 1e-9);
        assertTrue(split.fine().count() > 1);
    }

    @Test void massAndEnergyAreConservedBeforeRepresentation() {
        var random = new Random(1701);
        for (int i = 0; i < 2000; i++) {
            double volume = random.nextDouble() * 2;
            double energy = random.nextDouble() * 1_000_000;
            var material = new Fragmentation.Material(100 + random.nextDouble() * 8000,
                    100 + random.nextDouble() * 20_000, .005 + random.nextDouble() * .05, random.nextDouble());
            var split = SurfaceEjecta.release(volume, energy, material);
            assertEquals(volume * material.densityKgPerM3(), split.fine().totalMassKg() + split.coarseMassKg(), 1e-8);
            double preferredSpeed = random.nextDouble() * 100;
            double speed = split.coarseSpeedMps(preferredSpeed);
            double bulkKinetic = .5 * split.coarseMassKg() * speed * speed;
            assertTrue(split.fine().kineticEnergyJ() + split.fine().surfaceWorkJ() + bulkKinetic <= energy + 1e-7);
            assertTrue(speed <= preferredSpeed + 1e-10);
            assertEquals(split.fine(), SurfaceEjecta.release(volume, energy, material).fine());
        }
    }

    @Test void noVolumeOrNoEnergyDoesNotCreateFlyingFragments() {
        var material = new Fragmentation.Material(2600, 200, .05, .15);
        assertEquals(0, SurfaceEjecta.release(0, 1000, material).fine().count());
        var zero = SurfaceEjecta.release(1, 0, material);
        assertEquals(0, zero.fine().count());
        assertEquals(2600, zero.coarseMassKg());
        assertEquals(0, zero.coarseSpeedMps(20));
    }
}
