package me.mss1r.axiomata.ballistics;

import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ProjectileBlastResolver {
    private ProjectileBlastResolver() {
    }

    public static List<LivingEntity> applyImpactShockDamageAndCollect(
            ServerLevel level, Vec3 center, @Nullable Entity directSource,
            @Nullable Entity owner, ProjectilePhysicsProfile physics,
            @Nullable LivingEntity excludedTarget, double radius, float damage) {
        List<LivingEntity> damagedTargets = new ArrayList<>();
        if (radius <= 0.0D || damage <= 0.0F) {
            return damagedTargets;
        }

        AABB area = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, Entity::isAlive)) {
            if (target == directSource || target == owner || target == excludedTarget) {
                continue;
            }

            double distance = distanceToBounds(center, target.getBoundingBox());
            if (distance > radius) {
                continue;
            }

            double falloff = 1.0D - distance / radius;
            double exposure = Explosion.getSeenPercent(center, target);
            float scaledDamage = (float) (damage * Math.pow(falloff, 0.75D) * exposure);
            float appliedDamage = ProjectilePhysics.damageWithArmorPiercing(physics, scaledDamage, target);
            if (appliedDamage > 0.5F
                    && target.hurt(level.damageSources().explosion(directSource, owner), appliedDamage)) {
                damagedTargets.add(target);
            }
        }
        return damagedTargets;
    }

    /** The profile's shock around an impact, scaled by the base damage the projectile still carries. */
    public static List<LivingEntity> applyShock(ServerLevel level, Vec3 center, BallisticProjectile projectile,
                                                ProjectilePhysicsProfile physics,
                                                @Nullable LivingEntity excludedTarget, double speed) {
        ProjectilePhysicsProfile.Shock shock = physics.shock();
        double radius = Math.max(shock.radius(), shock.radiusAt(speed));
        float damage = (float) (projectile.getBaseDamage() * shock.damageAt(speed));
        return applyImpactShockDamageAndCollect(level, center, projectile, projectile.getOwner(),
                physics, excludedTarget, radius, damage);
    }

    private static double distanceToBounds(Vec3 point, AABB bounds) {
        double closestX = Mth.clamp(point.x, bounds.minX, bounds.maxX);
        double closestY = Mth.clamp(point.y, bounds.minY, bounds.maxY);
        double closestZ = Mth.clamp(point.z, bounds.minZ, bounds.maxZ);
        return point.distanceTo(new Vec3(closestX, closestY, closestZ));
    }

}
