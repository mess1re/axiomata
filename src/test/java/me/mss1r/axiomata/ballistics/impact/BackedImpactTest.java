package me.mss1r.axiomata.ballistics.impact;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class BackedImpactTest {
    private static final LayeredImpact.Layer BODY = new LayeredImpact.Layer(0.6, new Penetration.Material(300_000, 1000));
    private static final BackedImpact.Facing ARMOR = new BackedImpact.Facing(4, 0.2,
            new LayeredImpact.Layer(0.003, new Penetration.Material(300e6, 7800)));

    @Test void stoppedHeavyStoneStillDamagesBacking() {
        var result = BackedImpact.resolve(LayeredImpact.Shot.circular(26, 0.27), 30, ARMOR, 70, 0, BODY);
        assertFalse(result.passedThrough());
        assertTrue(result.bodyWorkJ() > 1000);
        assertTrue(result.facingWorkJ() > 0);
        assertTrue(result.bodyVelocityMps() > 0);
    }

    @Test void freePanelAndBackingConserveEnergyAndMomentum() {
        var random = new Random(2728373);
        for (int i = 0; i < 2000; i++) {
            double mass = 0.01 + random.nextDouble() * 150;
            double speed = 1 + random.nextDouble() * 330;
            double bodyVelocity = random.nextDouble() * 4 - 2;
            var result = BackedImpact.resolve(LayeredImpact.Shot.circular(mass, 0.01 + random.nextDouble() * 0.5),
                    speed, ARMOR, 70, bodyVelocity, BODY);
            double initial = energy(mass, speed) + energy(74, bodyVelocity);
            double after = energy(mass, result.projectileVelocityMps()) + energy(4, result.facingVelocityMps())
                    + energy(70, result.bodyVelocityMps());
            assertEquals(initial, after + result.facingWorkJ() + result.bodyWorkJ(), Math.max(1e-8, initial * 1e-12));
            double momentum = mass * speed + 74 * bodyVelocity;
            assertEquals(momentum, mass * result.projectileVelocityMps() + 4 * result.facingVelocityMps()
                    + 70 * result.bodyVelocityMps(), Math.max(1e-8, Math.abs(momentum) * 1e-12));
        }
    }

    @Test void uniformFrameShiftDoesNotChangeDamageWork() {
        var shot = LayeredImpact.Shot.circular(8, 0.12);
        var first = BackedImpact.resolve(shot, 300, ARMOR, 70, 0, BODY);
        var shifted = BackedImpact.resolve(shot, 310, ARMOR, 70, 10, BODY);
        assertEquals(first.bodyWorkJ(), shifted.bodyWorkJ(), 1e-8);
        assertEquals(first.facingWorkJ(), shifted.facingWorkJ(), 1e-8);
    }

    private static double energy(double mass, double speed) { return 0.5 * mass * speed * speed; }
}
