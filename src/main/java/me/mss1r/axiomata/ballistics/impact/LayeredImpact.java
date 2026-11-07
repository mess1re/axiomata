package me.mss1r.axiomata.ballistics.impact;

import java.util.ArrayList;
import java.util.List;

/**
 * One-dimensional impact of a rigid projectile on layers belonging to one translating target.
 * Momentum and resistance work are accounted for together. A fixed target has infinite mass.
 * There are no weapon identifiers, HP, armor points, explosions or area-damage rules here.
 * This is a penetration approximation, not a model of local tissue injury or structural failure.
 */
public final class LayeredImpact {
    private LayeredImpact() {}

    public record Shot(double massKg, double areaM2) {
        public Shot {
            Penetration.positive("massKg", massKg);
            Penetration.positive("areaM2", areaM2);
        }

        public static Shot circular(double massKg, double diameterM) {
            Penetration.positive("diameterM", diameterM);
            return new Shot(massKg, Math.PI * diameterM * diameterM * 0.25D);
        }

        public double speedAtEnergy(double energyJ) {
            Penetration.nonNegative("energyJ", energyJ);
            return Math.sqrt(2.0D * energyJ / massKg);
        }
    }

    public record Layer(double lengthM, Penetration.Material material) {
        public Layer {
            Penetration.nonNegative("lengthM", lengthM);
            java.util.Objects.requireNonNull(material);
        }
    }

    public record LayerResult(int index, Penetration.Passage passage, double impulseNs) {}

    public record Result(double projectileVelocityMps, double targetVelocityMps, double workJ,
                         double impulseNs, boolean passedThrough, List<LayerResult> layers) {
        public Result {
            layers = List.copyOf(layers);
        }
    }

    /**
     * Velocities are signed along the impact trajectory. The projectile must be approaching the target.
     * Armor attached to a body shares that body's mass; an independent cover is a separate resolve call.
     * An embedded projectile retains the common translational velocity, not an artificial zero world speed.
     */
    public static Result resolve(Shot shot, double projectileVelocityMps, double targetMassKg,
                                 double targetVelocityMps, List<Layer> layers) {
        if (!Double.isFinite(projectileVelocityMps) || !Double.isFinite(targetVelocityMps)) {
            throw new IllegalArgumentException("velocities must be finite");
        }
        if (!(targetMassKg > 0.0D)) {
            throw new IllegalArgumentException("targetMassKg must be positive or positive infinity");
        }
        List<Layer> targetLayers = List.copyOf(layers);
        double reducedMass = reducedMass(shot.massKg(), targetMassKg);
        double projectileVelocity = projectileVelocityMps;
        double targetVelocity = targetVelocityMps;
        double totalWork = 0.0D;
        double totalImpulse = 0.0D;
        var results = new ArrayList<LayerResult>();
        for (int index = 0; index < targetLayers.size(); index++) {
            double closingSpeed = projectileVelocity - targetVelocity;
            if (!(closingSpeed > 0.0D)) {
                return new Result(projectileVelocity, targetVelocity, totalWork, totalImpulse, false, results);
            }
            Layer layer = targetLayers.get(index);
            var passage = Penetration.through(reducedMass, shot.areaM2(), closingSpeed,
                    layer.lengthM(), layer.material());
            // Equivalent to reducedMass * (entrySpeed - exitSpeed), without cancellation on thin layers.
            double impulse = 2.0D * passage.workJ() / (closingSpeed + passage.exitSpeedMps());
            projectileVelocity -= impulse / shot.massKg();
            targetVelocity += impulse / targetMassKg;
            totalWork += passage.workJ();
            totalImpulse += impulse;
            results.add(new LayerResult(index, passage, impulse));
            if (!passage.exited()) {
                return new Result(projectileVelocity, targetVelocity, totalWork, totalImpulse, false, results);
            }
        }
        return new Result(projectileVelocity, targetVelocity, totalWork, totalImpulse, true, results);
    }

    private static double reducedMass(double projectileMass, double targetMass) {
        double smaller = Math.min(projectileMass, targetMass);
        double larger = Math.max(projectileMass, targetMass);
        return smaller / (1.0D + smaller / larger);
    }
}
