package me.mss1r.axiomata.ballistics.impact;

/** Separates surface chips from bulk before either fraction is represented in the world. */
public final class SurfaceEjecta {
    private SurfaceEjecta() {}

    public record Partition(Fragmentation.Cloud fine, double coarseMassKg, double coarseEnergyBudgetJ) {
        public double coarseSpeedMps(double preferredSpeedMps) {
            Penetration.nonNegative("preferredSpeedMps", preferredSpeedMps);
            return coarseMassKg > 0 ? Math.min(preferredSpeedMps, Math.sqrt(2 * coarseEnergyBudgetJ / coarseMassKg)) : 0;
        }
    }

    /** A compact piece exposes a face of V^(2/3). At most one material-grain-thick layer becomes fine ejecta. */
    public static Partition release(double volumeM3, double availableEnergyJ, Fragmentation.Material material) {
        Penetration.nonNegative("volumeM3", volumeM3);
        Penetration.nonNegative("availableEnergyJ", availableEnergyJ);
        double layer = availableEnergyJ > 0 ? Math.min(volumeM3, Math.pow(volumeM3, 2.0 / 3.0) * material.grainSizeM()) : 0;
        var fine = Fragmentation.release(layer, availableEnergyJ, material);
        double totalMass = volumeM3 * material.densityKgPerM3();
        return new Partition(fine, Math.max(0, totalMass - fine.totalMassKg()), fine.remainingEnergyJ());
    }
}
