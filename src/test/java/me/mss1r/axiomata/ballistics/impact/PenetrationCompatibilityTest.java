package me.mss1r.axiomata.ballistics.impact;

import me.mss1r.axiomata.ballistics.ImpactResolver;
import me.mss1r.axiomata.ballistics.ImpactResults;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PenetrationCompatibilityTest {
    @Test
    void fixedRigidTargetMatchesExistingPonceletPenetration() {
        for (double mass : List.of(0.02, 0.2, 8.0, 26.0, 136.0, 150.0)) {
            for (double diameter : List.of(0.017, 0.03, 0.12, 0.27, 0.46, 0.49)) {
                for (double speed : List.of(5.0, 30.0, 120.0, 200.0, 330.0)) {
                    for (double length : List.of(0.003, 0.2, 0.6, 1.0)) {
                        for (double drag : List.of(0.0, 625.0, 1000.0)) {
                            var oldMaterial = new ImpactResults.Material(300_000, drag, 0, 1, 1);
                            double oldSpeed = ImpactResolver.speedThrough(oldMaterial, mass, diameter, speed, length);
                            var shot = LayeredImpact.Shot.circular(mass, diameter);
                            var result = LayeredImpact.resolve(shot, speed, Double.POSITIVE_INFINITY, 0,
                                    List.of(new LayeredImpact.Layer(length, new Penetration.Material(300_000, drag))));
                            assertEquals(oldSpeed, result.projectileVelocityMps(), 1e-9);
                        }
                    }
                }
            }
        }
    }
}
