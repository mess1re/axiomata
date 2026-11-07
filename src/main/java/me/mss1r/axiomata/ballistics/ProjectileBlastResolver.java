package me.mss1r.axiomata.ballistics;

import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/** Chemical splash keeps its calibrated radial falloff; inert impacts use fragments instead. */
public final class ProjectileBlastResolver {
    private ProjectileBlastResolver() {}

    public static List<LivingEntity> applyBlast(ServerLevel level, Vec3 center, ImpactSource source,
                                               ProjectilePhysicsProfile physics, double speedBlocksPerTick) {
        var reached = new ArrayList<LivingEntity>();
        var blast = physics.blast();
        double speed = Math.max(0, speedBlocksPerTick);
        double radius = blast.radius() * Math.max(1, Mth.clamp(.85 + Math.log1p(speed) * .18, .75, 1.45));
        double damage = blast.damage() * Mth.clamp(.85 + Math.log1p(speed) * .2, .75, 1.55);
        if (!(radius > 0 && damage > 0)) return reached;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius), entity -> entity.isAlive() && source.mayHit().test(entity))) {
            double distance = Math.sqrt(target.getBoundingBox().distanceToSqr(center));
            if (distance > radius) continue;
            float raw = (float) (damage * Math.pow(1 - distance / radius, .75) * Explosion.getSeenPercent(center, target));
            float applied = target instanceof StructuralImpactTarget ? raw
                    : ProjectilePhysics.damageWithArmorPiercing(physics.directHit().armorPiercing(), raw, target);
            if (applied > .5 && target.hurt(ProjectileCombat.source(level, ProjectileCombat.PHYSICAL_BLAST,
                    source.direct(), source.owner()), applied)) reached.add(target);
        }
        return reached;
    }
}
