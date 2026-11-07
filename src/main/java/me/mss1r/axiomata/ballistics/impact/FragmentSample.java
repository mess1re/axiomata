package me.mss1r.axiomata.ballistics.impact;

/** A Monte Carlo sample is one physical fragment with a statistical weight, never a larger projectile. */
public record FragmentSample(double massKg, double diameterM, double weight) {
    public static FragmentSample of(Fragmentation.Cloud cloud, int trajectories) {
        if (trajectories <= 0 || trajectories > cloud.count()) throw new IllegalArgumentException("invalid sample count");
        return new FragmentSample(cloud.fragmentMassKg(), cloud.fragmentDiameterM(), (double) cloud.count() / trajectories);
    }

    public double damage(DamageScale scale, double workPerFragmentJ) {
        return scale.damage(workPerFragmentJ) * weight;
    }
}
