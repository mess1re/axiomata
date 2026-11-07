package me.mss1r.axiomata.ballistics.impact;

/**
 * Rigid-projectile penetration with resistance {@code area * (strength + inertialDrag * speed^2)}.
 * All inputs use SI units. Material coefficients require calibration for this resistance law;
 * inertial drag is not necessarily the material's density or its aerodynamic drag coefficient.
 * Projectile deformation, rotation, rebound and fragmentation are outside this model.
 */
public final class Penetration {
    private Penetration() {}

    public record Material(double strengthPa, double inertialDragKgPerM3) {
        public Material {
            nonNegative("strengthPa", strengthPa);
            nonNegative("inertialDragKgPerM3", inertialDragKgPerM3);
        }
    }

    /** Work is the loss of relative kinetic energy, not an additional source of damage energy. */
    public record Passage(double exitSpeedMps, double distanceM, double workJ, boolean exited) {}

    /**
     * Integrates the resistance law over a path through homogeneous material.
     * With a movable target, pass the reduced mass and the relative entry speed.
     * Area is the presented cross-section; length is measured along the trajectory, not the surface normal.
     */
    public static Passage through(double massKg, double areaM2, double entrySpeedMps,
                                  double lengthM, Material material) {
        positive("massKg", massKg);
        positive("areaM2", areaM2);
        nonNegative("entrySpeedMps", entrySpeedMps);
        nonNegative("lengthM", lengthM);
        double energy = 0.5D * massKg * entrySpeedMps * entrySpeedMps;
        if (!Double.isFinite(energy)) {
            throw new IllegalArgumentException("entry kinetic energy must be finite");
        }
        if (lengthM == 0.0D) {
            return new Passage(entrySpeedMps, 0.0D, 0.0D, true);
        }
        if (energy == 0.0D) {
            return new Passage(0.0D, 0.0D, 0.0D, false);
        }
        double strengthForce = material.strengthPa() * areaM2;
        double drag = material.inertialDragKgPerM3() * areaM2;
        double stoppingDistance = Double.POSITIVE_INFINITY;
        if (strengthForce > 0.0D) {
            double ratio = 2.0D * drag * energy / (massKg * strengthForce);
            // log1p(ratio)/ratio tends to one as inertial resistance vanishes.
            double correction = ratio == 0.0D ? 1.0D : Math.log1p(ratio) / ratio;
            stoppingDistance = energy / strengthForce * correction;
        }
        if (lengthM >= stoppingDistance) {
            return new Passage(0.0D, stoppingDistance, energy, false);
        }
        double depth = 2.0D * drag * lengthM / massKg;
        double attenuation = -Math.expm1(-depth);
        double strengthIntegral = depth == 0.0D ? 1.0D : attenuation / depth;
        double work = Math.min(energy, energy * attenuation + strengthForce * lengthM * strengthIntegral);
        // Compute retained energy directly: subtracting work from entry energy loses very small exit speeds.
        double exitEnergy = energy * Math.exp(-depth) - strengthForce * lengthM * strengthIntegral;
        double exitSpeed = Math.sqrt(2.0D * Math.max(0.0D, exitEnergy) / massKg);
        return new Passage(exitSpeed, lengthM, work, exitSpeed > 0.0D);
    }

    static void positive(String name, double value) {
        if (!(value > 0.0D) || !Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }

    static void nonNegative(String name, double value) {
        if (!(value >= 0.0D) || !Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}
