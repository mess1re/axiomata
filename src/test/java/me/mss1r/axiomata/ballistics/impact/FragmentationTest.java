package me.mss1r.axiomata.ballistics.impact;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FragmentationTest {
    private static final Fragmentation.Material STONE = new Fragmentation.Material(2600, 200, 0.05, 0.15);

    @Test void accountsForAllMassAndAvailableEnergy() {
        for (double volume : new double[]{1e-6, 0.001, 0.1, 1, 20}) {
            for (double energy : new double[]{0.001, 1, 100, 10000, 1e7}) {
                var cloud = Fragmentation.release(volume, energy, STONE);
                assertEquals(2600 * volume, cloud.totalMassKg(), 2600 * volume * 1e-12);
                assertEquals(energy, cloud.kineticEnergyJ() + cloud.surfaceWorkJ() + cloud.remainingEnergyJ(), energy * 1e-12);
                assertEquals(cloud.kineticEnergyJ(), 0.5 * cloud.totalMassKg() * Math.pow(cloud.speedMps(), 2), energy * 1e-12);
                assertTrue(cloud.count() >= 1);
                assertTrue(cloud.surfaceWorkJ() >= 0 && cloud.remainingEnergyJ() >= 0);
            }
        }
    }

    @Test void noVolumeOrEnergyDoesNotEmitDamagingFragments() {
        assertEquals(0, Fragmentation.release(0, 10000, STONE).count());
        assertEquals(0, Fragmentation.release(1, 0, STONE).count());
    }

    @Test void greaterEnergyCanBreakIntoMoreFragmentsButDoesNotExceedGrainLimit() {
        long previous = 0;
        for (double energy : new double[]{1, 100, 1000, 1e4, 1e5, 1e6}) {
            var cloud = Fragmentation.release(1, energy, STONE);
            assertTrue(cloud.count() >= previous);
            assertTrue(cloud.count() <= Math.ceil(1 / Math.pow(0.05, 3)));
            previous = cloud.count();
        }
    }

    @Test void tougherNewSurfacesProduceFewerFragments() {
        var soft = Fragmentation.release(1, 1000, STONE);
        var tough = Fragmentation.release(1, 1000, new Fragmentation.Material(2600, 20000, 0.05, 0.15));
        assertTrue(tough.count() < soft.count());
        assertEquals(soft.speedMps(), tough.speedMps());
    }

    @Test void velocityRespondsToEnergyPerUnitMass() {
        var first = Fragmentation.release(1, 1000, STONE);
        var fourEnergy = Fragmentation.release(1, 4000, STONE);
        var fourMass = Fragmentation.release(4, 1000, STONE);
        assertEquals(first.speedMps() * 2, fourEnergy.speedMps(), 1e-12);
        assertEquals(first.speedMps() / 2, fourMass.speedMps(), 1e-12);
    }

    @Test void rejectsInvalidCalibrations() {
        assertThrows(IllegalArgumentException.class, () -> new Fragmentation.Material(0, 200, 0.05, 0.15));
        assertThrows(IllegalArgumentException.class, () -> new Fragmentation.Material(2600, 200, 0.05, 1.1));
        assertThrows(IllegalArgumentException.class, () -> Fragmentation.release(1, Double.NaN, STONE));
    }
}
