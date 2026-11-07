package me.mss1r.axiomata.ballistics.impact;

/** Mass and energy accounting for fragmentation of a known volume, before sampling flight directions. */
public final class Fragmentation {
    private Fragmentation() {}

    /** Material calibration, not per-weapon damage: density, new-surface work, minimum grain size and ejection share. */
    public record Material(double densityKgPerM3, double surfaceEnergyJPerM2,
                           double grainSizeM, double ejectionFraction) {
        public Material {
            Penetration.positive("densityKgPerM3", densityKgPerM3);
            Penetration.positive("surfaceEnergyJPerM2", surfaceEnergyJPerM2);
            Penetration.positive("grainSizeM", grainSizeM);
            Penetration.nonNegative("ejectionFraction", ejectionFraction);
            if (ejectionFraction > 1) throw new IllegalArgumentException("ejectionFraction must not exceed one");
        }
    }

    /** Physical fragment count is independent of how many representative trajectories the server samples. */
    public record Cloud(long count, double fragmentMassKg, double fragmentDiameterM, double speedMps,
                        double kineticEnergyJ, double surfaceWorkJ, double remainingEnergyJ) {
        public double totalMassKg() { return count * fragmentMassKg; }
    }

    /**
     * The caller has already paid for freeing this volume from the target. Remaining work is divided between
     * creation of new surfaces and ejection. Equiaxed grains approximate the material's fragment-size limit.
     * The law does not infer a real size distribution from impact energy alone.
     */
    public static Cloud release(double volumeM3, double availableEnergyJ, Material material) {
        Penetration.nonNegative("volumeM3", volumeM3);
        Penetration.nonNegative("availableEnergyJ", availableEnergyJ);
        if (volumeM3 == 0 || availableEnergyJ == 0) return new Cloud(0, 0, 0, 0, 0, 0, availableEnergyJ);
        double kinetic = availableEnergyJ * material.ejectionFraction();
        return fromBudgets(volumeM3, availableEnergyJ - kinetic, kinetic, material);
    }

    public static Cloud fromBudgets(double volumeM3, double surfaceBudget, double kinetic, Material material) {
        Penetration.nonNegative("volumeM3", volumeM3);
        Penetration.nonNegative("surfaceBudget", surfaceBudget);
        Penetration.nonNegative("kinetic", kinetic);
        double availableEnergyJ = surfaceBudget + kinetic;
        if (volumeM3 == 0 || availableEnergyJ == 0) return new Cloud(0, 0, 0, 0, 0, 0, availableEnergyJ);
        double surfaceUnit = 6 * material.surfaceEnergyJPerM2() * Math.pow(volumeM3, 2.0 / 3.0);
        double affordable = Math.pow(1 + surfaceBudget / surfaceUnit, 3);
        double grains = volumeM3 / Math.pow(material.grainSizeM(), 3);
        long count = (long) Math.max(1, Math.min(Long.MAX_VALUE, Math.min(Math.floor(affordable), Math.floor(grains))));
        double surface = Math.min(surfaceBudget, surfaceUnit * (Math.cbrt(count) - 1));
        double mass = material.densityKgPerM3() * volumeM3;
        double diameter = Math.cbrt(6 * volumeM3 / (Math.PI * count));
        return new Cloud(count, mass / count, diameter, Math.sqrt(2 * kinetic / mass),
                kinetic, surface, Math.max(0, availableEnergyJ - kinetic - surface));
    }
}
