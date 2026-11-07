package me.mss1r.axiomata.ballistics.impact;

/**
 * Explicit gameplay calibration from selected resistance work to HP. This is not a physical injury law.
 * Callers select the work spent in relevant target layers; work in cover/armor is not body damage.
 * No default calibration is imposed by the penetration model.
 */
public record DamageScale(double referenceEnergyJ, double referenceDamageHp, double exponent) {
    public DamageScale {
        Penetration.positive("referenceEnergyJ", referenceEnergyJ);
        Penetration.positive("referenceDamageHp", referenceDamageHp);
        Penetration.positive("exponent", exponent);
    }

    public double damage(double workJ) {
        Penetration.nonNegative("workJ", workJ);
        return referenceDamageHp * Math.pow(workJ / referenceEnergyJ, exponent);
    }
}
