package me.mss1r.axiomata.ballistics.impact;

/** Gameplay HP at a declared reference speed; independent of penetration and secondary fragments. */
public record DirectDamage(double damageHp, double referenceSpeedMps) {
    public DirectDamage {
        Penetration.nonNegative("damageHp", damageHp);
        Penetration.positive("referenceSpeedMps", referenceSpeedMps);
    }

    public double atSpeed(double speedMps) {
        Penetration.nonNegative("speedMps", speedMps);
        double ratio = speedMps / referenceSpeedMps;
        return damageHp * ratio * ratio;
    }
}
