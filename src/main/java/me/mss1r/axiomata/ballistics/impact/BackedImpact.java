package me.mss1r.axiomata.ballistics.impact;

import java.util.List;

/**
 * A finite facing panel backed by a separate deformable body. Panel motion is carried into the backing,
 * including when the projectile remains embedded in the panel. No armor-piercing percentage is used.
 * This lumped, one-dimensional approximation does not model plate bending or a spatial stress field.
 */
public final class BackedImpact {
    private BackedImpact() {}

    public record Facing(double massKg, double areaM2, LayeredImpact.Layer layer) {
        public Facing {
            Penetration.positive("massKg", massKg);
            Penetration.positive("areaM2", areaM2);
            java.util.Objects.requireNonNull(layer);
        }
    }

    public record Result(double projectileVelocityMps, double facingVelocityMps, double bodyVelocityMps,
                         double facingWorkJ, double bodyWorkJ, boolean passedThrough,
                         double retainedProjectileMassKg) {}

    public static Result resolve(LayeredImpact.Shot shot, double entryVelocityMps, Facing facing,
                                 double bodyMassKg, double bodyVelocityMps, LayeredImpact.Layer body) {
        var front = LayeredImpact.resolve(shot, entryVelocityMps, facing.massKg(), bodyVelocityMps,
                List.of(facing.layer()));
        if (!front.passedThrough()) {
            double coupledMass = shot.massKg() + facing.massKg();
            var load = LayeredImpact.resolve(new LayeredImpact.Shot(coupledMass, facing.areaM2()),
                    front.targetVelocityMps(), bodyMassKg, bodyVelocityMps, List.of(body));
            return new Result(load.projectileVelocityMps(), load.projectileVelocityMps(), load.targetVelocityMps(),
                    front.workJ(), load.workJ(), false, coupledMass);
        }
        var penetration = LayeredImpact.resolve(shot, front.projectileVelocityMps(), bodyMassKg,
                bodyVelocityMps, List.of(body));
        double backingMass = bodyMassKg + (penetration.passedThrough() ? 0 : shot.massKg());
        var load = LayeredImpact.resolve(new LayeredImpact.Shot(facing.massKg(), facing.areaM2()),
                front.targetVelocityMps(), backingMass, penetration.targetVelocityMps(), List.of(body));
        return new Result(penetration.passedThrough() ? penetration.projectileVelocityMps() : load.targetVelocityMps(),
                load.projectileVelocityMps(), load.targetVelocityMps(),
                front.workJ(), penetration.workJ() + load.workJ(), penetration.passedThrough(), shot.massKg());
    }
}
