package me.mss1r.axiomata.ballistics.impact;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class LayeredImpactTest {
    private static final Penetration.Material BODY = new Penetration.Material(300_000, 1000);

    @Test
    void constantForceStopsAtWorkOverForce() {
        var passage = Penetration.through(2, 0.01, 10, 100, new Penetration.Material(1000, 0));
        assertEquals(10, passage.distanceM(), 1e-12);
        assertEquals(100, passage.workJ(), 1e-12);
        assertEquals(0, passage.exitSpeedMps());
        assertFalse(passage.exited());
    }

    @Test
    void inertialResistanceAloneDecaysSpeedExponentially() {
        var passage = Penetration.through(2, 0.01, 100, 0.5, new Penetration.Material(0, 1000));
        assertEquals(100 * Math.exp(-2.5), passage.exitSpeedMps(), 1e-11);
        assertEquals(0.5, passage.distanceM());
        assertTrue(passage.exited());
    }

    @Test
    void veryStrongDragDoesNotRoundFiniteExitSpeedToZero() {
        var passage = Penetration.through(2, 0.01, 100, 10, new Penetration.Material(0, 1000));
        assertEquals(100 * Math.exp(-50), passage.exitSpeedMps(), 1e-32);
        assertTrue(passage.exited());
    }

    @Test
    void integratedResistanceAgreesWithIndependentSpatialIntegration() {
        // Integrate dE/dx = -A * (Y + 2*rho*E/m) numerically, independently of the closed form.
        double energy = 0.5 * 2 * 100 * 100;
        double dx = 0.2 / 100_000;
        for (int step = 0; step < 100_000; step++) {
            energy -= 0.001 * (300_000 + 2 * 1000 * energy / 2) * dx;
        }
        var passage = Penetration.through(2, 0.001, 100, 0.2, BODY);
        assertEquals(energy, 0.5 * 2 * Math.pow(passage.exitSpeedMps(), 2), 0.01);
    }

    @Test
    void transparentLayerAndZeroLengthDoNotSpendEnergy() {
        assertEquals(new Penetration.Passage(100, 10, 0, true),
                Penetration.through(1, 0.1, 100, 10, new Penetration.Material(0, 0)));
        assertEquals(new Penetration.Passage(100, 0, 0, true),
                Penetration.through(1, 0.1, 100, 0, BODY));
        assertEquals(new Penetration.Passage(0, 0, 0, false),
                Penetration.through(1, 0.1, 0, 10, BODY));
    }

    @Test
    void vanishingDragHasTheConstantForceLimit() {
        var zeroDrag = Penetration.through(1, 0.001, 100, 0.2, new Penetration.Material(300_000, 0));
        var tinyDrag = Penetration.through(1, 0.001, 100, 0.2, new Penetration.Material(300_000, 1e-20));
        assertEquals(zeroDrag.workJ(), tinyDrag.workJ(), 1e-10);
        assertEquals(zeroDrag.exitSpeedMps(), tinyDrag.exitSpeedMps(), 1e-10);
    }

    @Test
    void thinLayersRetainSmallNonZeroWorkAndImpulse() {
        var result = LayeredImpact.resolve(LayeredImpact.Shot.circular(8, 0.12), 300,
                Double.POSITIVE_INFINITY, 0, List.of(new LayeredImpact.Layer(1e-12, BODY)));
        assertTrue(result.workJ() > 0);
        assertTrue(result.impulseNs() > 0);
        assertTrue(result.passedThrough());
    }

    @Test
    void fixedTargetUsesProjectileMassAndKeepsItsVelocity() {
        var shot = LayeredImpact.Shot.circular(8, 0.12);
        var passage = Penetration.through(8, shot.areaM2(), 300, 0.6, BODY);
        var result = LayeredImpact.resolve(shot, 300, Double.POSITIVE_INFINITY, 0,
                List.of(new LayeredImpact.Layer(0.6, BODY)));
        assertEquals(passage.exitSpeedMps(), result.projectileVelocityMps(), 1e-10);
        assertEquals(passage.workJ(), result.workJ(), 1e-10);
        assertEquals(0, result.targetVelocityMps());
    }

    @Test
    void embeddedProjectileAndFreeTargetShareTheirCenterOfMassVelocity() {
        var result = LayeredImpact.resolve(LayeredImpact.Shot.circular(2, 0.1), 20, 8, 0,
                List.of(new LayeredImpact.Layer(100, BODY)));
        assertFalse(result.passedThrough());
        assertEquals(4, result.projectileVelocityMps(), 1e-12);
        assertEquals(4, result.targetVelocityMps(), 1e-12);
        // Initial 400 J = 320 J work + 80 J retained translational kinetic energy.
        assertEquals(320, result.workJ(), 1e-10);
        assertEquals(32, result.impulseNs(), 1e-12);
    }

    @Test
    void conservesMomentumAndAccountsForEnergyAcrossMassesAndLayers() {
        var random = new Random(879225);
        for (int index = 0; index < 2000; index++) {
            double projectileMass = Math.pow(10, -2 + random.nextDouble() * 5);
            double targetMass = Math.pow(10, -1 + random.nextDouble() * 5);
            double projectileVelocity = 10 + random.nextDouble() * 500;
            double targetVelocity = -50 + random.nextDouble() * 55;
            var shot = LayeredImpact.Shot.circular(projectileMass, 0.005 + random.nextDouble() * 0.5);
            var layers = List.of(new LayeredImpact.Layer(0.003, new Penetration.Material(500e6, 7800)),
                    new LayeredImpact.Layer(0.1 + random.nextDouble(), BODY));
            var result = LayeredImpact.resolve(shot, projectileVelocity, targetMass, targetVelocity, layers);
            double initialEnergy = energy(projectileMass, projectileVelocity) + energy(targetMass, targetVelocity);
            double finalEnergy = energy(projectileMass, result.projectileVelocityMps())
                    + energy(targetMass, result.targetVelocityMps());
            assertEquals(initialEnergy, finalEnergy + result.workJ(), Math.max(1e-8, initialEnergy * 1e-12));
            double initialMomentum = projectileMass * projectileVelocity + targetMass * targetVelocity;
            assertEquals(initialMomentum, projectileMass * result.projectileVelocityMps()
                    + targetMass * result.targetVelocityMps(), Math.max(1e-8, Math.abs(initialMomentum) * 1e-12));
            assertTrue(result.workJ() >= 0);
            double reducedMass = 1 / (1 / projectileMass + 1 / targetMass);
            assertTrue(result.workJ() <= energy(reducedMass, projectileVelocity - targetVelocity) * (1 + 1e-12));
        }
    }

    @Test
    void referenceFrameChangesOnlyAbsoluteVelocities() {
        var shot = LayeredImpact.Shot.circular(150, 0.49);
        var layers = List.of(new LayeredImpact.Layer(0.6, BODY));
        var first = LayeredImpact.resolve(shot, 315, 70, 0, layers);
        var shifted = LayeredImpact.resolve(shot, 415, 70, 100, layers);
        assertEquals(first.workJ(), shifted.workJ(), 1e-9);
        assertEquals(first.impulseNs(), shifted.impulseNs(), 1e-9);
        assertEquals(first.projectileVelocityMps() + 100, shifted.projectileVelocityMps(), 1e-9);
        assertEquals(first.targetVelocityMps() + 100, shifted.targetVelocityMps(), 1e-9);
    }

    @Test
    void subdividingOneMaterialDoesNotChangeTheImpact() {
        var shot = LayeredImpact.Shot.circular(8, 0.12);
        var whole = LayeredImpact.resolve(shot, 300, 70, 0, List.of(new LayeredImpact.Layer(0.6, BODY)));
        var split = LayeredImpact.resolve(shot, 300, 70, 0, List.of(new LayeredImpact.Layer(0.2, BODY),
                new LayeredImpact.Layer(0.3, BODY), new LayeredImpact.Layer(0.1, BODY)));
        assertEquals(whole.workJ(), split.workJ(), 1e-9);
        assertEquals(whole.projectileVelocityMps(), split.projectileVelocityMps(), 1e-10);
        assertEquals(whole.targetVelocityMps(), split.targetVelocityMps(), 1e-10);
    }

    @Test
    void stoppedShotDoesNotReachOrDamageTheNextLayer() {
        var shot = LayeredImpact.Shot.circular(0.02, 0.017);
        var result = LayeredImpact.resolve(shot, 200, 70, 0, List.of(
                new LayeredImpact.Layer(0.1, new Penetration.Material(500e6, 7800)),
                new LayeredImpact.Layer(0.6, BODY)));
        assertFalse(result.passedThrough());
        assertEquals(1, result.layers().size());
        assertEquals(0, result.layers().get(0).index());
        assertTrue(result.projectileVelocityMps() > 0); // Embedded does not mean stationary in world space.
    }

    @Test
    void strongerArmorReducesWorkInTheFollowingBodyLayer() {
        var shot = LayeredImpact.Shot.circular(0.02, 0.017);
        var bare = LayeredImpact.resolve(shot, 200, 70, 0, List.of(new LayeredImpact.Layer(0.6, BODY)));
        var armored = LayeredImpact.resolve(shot, 200, 70, 0, List.of(
                new LayeredImpact.Layer(0.003, new Penetration.Material(500e6, 7800)),
                new LayeredImpact.Layer(0.6, BODY)));
        double bodyWork = armored.layers().stream().filter(layer -> layer.index() == 1)
                .mapToDouble(layer -> layer.passage().workJ()).sum();
        assertTrue(bodyWork < bare.workJ());
        assertEquals(armored.workJ(), armored.layers().stream().mapToDouble(layer -> layer.passage().workJ()).sum());
    }

    @Test
    void subsequentTargetReceivesOnlyTheRemainingProjectileEnergy() {
        var shot = LayeredImpact.Shot.circular(8, 0.12);
        var cover = LayeredImpact.resolve(shot, 300, Double.POSITIVE_INFINITY, 0,
                List.of(new LayeredImpact.Layer(0.2, new Penetration.Material(16.7e6, 625))));
        var body = LayeredImpact.resolve(shot, cover.projectileVelocityMps(), 70, 0,
                List.of(new LayeredImpact.Layer(0.6, BODY)));
        double initialEnergy = energy(8, 300);
        assertEquals(initialEnergy, cover.workJ() + body.workJ() + energy(8, body.projectileVelocityMps())
                + energy(70, body.targetVelocityMps()), 1e-8);
        assertTrue(body.workJ() < initialEnergy - cover.workJ());
    }

    @Test
    void projectileCannotHitATargetItIsNotCatchingUpWith() {
        var result = LayeredImpact.resolve(LayeredImpact.Shot.circular(1, 0.01), 10, 70, 20,
                List.of(new LayeredImpact.Layer(0.6, BODY)));
        assertEquals(0, result.workJ());
        assertEquals(0, result.impulseNs());
        assertTrue(result.layers().isEmpty());
        assertFalse(result.passedThrough());
    }

    @Test
    void damageCalibrationDoesNotChangeThePhysicalResult() {
        var linear = new DamageScale(400, 20, 1);
        var squareRoot = new DamageScale(400, 20, 0.5);
        assertEquals(0, linear.damage(0));
        assertEquals(20, linear.damage(400));
        assertEquals(20, squareRoot.damage(400));
        assertEquals(80, linear.damage(1600));
        assertEquals(40, squareRoot.damage(1600));
    }

    @Test
    void launchSpeedFollowsEnergyAndMass() {
        assertEquals(200, LayeredImpact.Shot.circular(0.02, 0.017).speedAtEnergy(400));
        assertEquals(100, LayeredImpact.Shot.circular(0.08, 0.017).speedAtEnergy(400));
        assertEquals(0, LayeredImpact.Shot.circular(0.02, 0.017).speedAtEnergy(0));
    }

    @Test
    void rejectsInvalidPhysicalInputs() {
        assertThrows(IllegalArgumentException.class, () -> new Penetration.Material(-1, 1000));
        assertThrows(IllegalArgumentException.class, () -> new Penetration.Material(0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> LayeredImpact.Shot.circular(0, 0.01));
        assertThrows(IllegalArgumentException.class, () -> LayeredImpact.Shot.circular(1, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new DamageScale(0, 20, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new DamageScale(400, 20, -1));
        assertThrows(IllegalArgumentException.class, () -> new DamageScale(400, 20, 0.5).damage(Double.NaN));
        var shot = LayeredImpact.Shot.circular(1, 0.01);
        assertThrows(IllegalArgumentException.class, () -> LayeredImpact.resolve(shot, 100, Double.NaN, 0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> LayeredImpact.resolve(shot, 100, -1, 0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> LayeredImpact.resolve(shot, 100, 70, Double.NaN, List.of()));
        assertThrows(IllegalArgumentException.class, () -> Penetration.through(1, 0.001, 100, -1, BODY));
    }

    private static double energy(double mass, double velocity) {
        return 0.5 * mass * velocity * velocity;
    }
}
