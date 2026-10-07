package me.mss1r.axiomata.ballistics;

import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;

/** Direct-hit damage, armor penetration and speed retained after passing through a body. */
public final class ProjectilePhysics {
    /** Body material for penetration: strength in Pa, drag in kg/m³, same as blocks. */
    private static final double BODY_STRENGTH = 300_000.0D;
    private static final double BODY_DRAG = 1000.0D;
    /** Strength added per armor point, in Pa. */
    private static final double ARMOR_STRENGTH = 200_000.0D;
    /** Below this speed, in m/s, a projectile stays in the body it hit. */
    private static final double LEAST_FLIGHT_SPEED = 5.0D;

    private ProjectilePhysics() {
    }

    public static float entityDamage(ProjectilePhysicsProfile profile, float baseDamage, double speed, LivingEntity target, boolean structuralTarget) {
        double speedMultiplier = Mth.clamp(0.78 + Math.log1p(Math.max(0.0, speed)) * 0.24, 0.65, 1.75);
        float rawDamage = (float) (baseDamage * profile.entity().damage() * speedMultiplier);
        if (structuralTarget) {
            return (float) (Math.max(0.0F, rawDamage) * profile.entity().structure());
        }
        return damageWithArmorPiercing(profile, rawDamage, target);
    }

    public static float damageWithArmorPiercing(ProjectilePhysicsProfile profile, float rawDamage,
                                                 LivingEntity target) {
        float damage = Math.max(0.0F, rawDamage);
        float piercing = Mth.clamp((float) profile.entity().armorPiercing(), 0.0F, 1.0F);
        float armor = target.getArmorValue();
        float toughness = (float) armorToughness(target);
        if (damage <= 0.0F || piercing <= 0.0F || armor <= 0.0F) {
            return damage;
        }

        float absorbedDamage = armorAdjustedDamage(target, damage, armor, toughness);
        float desiredDamage = Mth.lerp(piercing, absorbedDamage, damage);
        if (desiredDamage <= absorbedDamage + 1.0E-4F) {
            return damage;
        }

        float low = damage;
        float high = Math.max(damage + 1.0F, damage * 2.0F);
        while (armorAdjustedDamage(target, high, armor, toughness) < desiredDamage && high < 1_000_000.0F) {
            high *= 2.0F;
        }
        for (int iteration = 0; iteration < 18; iteration++) {
            float middle = (low + high) * 0.5F;
            if (armorAdjustedDamage(target, middle, armor, toughness) < desiredDamage) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return high;
    }

    private static float armorAdjustedDamage(LivingEntity target, float damage, float armor, float toughness) {
        //? if forge {
        /*return CombatRules.getDamageAfterAbsorb(damage, armor, toughness);
        *///?} else {
        return CombatRules.getDamageAfterAbsorb(
                target, damage, target.damageSources().generic(), armor, toughness);
        //?}
    }

    /** Remaining speed after passing through a body, in blocks/tick like {@code currentSpeed}; zero if stopped. */
    public static double remainingEntityPenetrationSpeed(BallisticProjectile projectile, LivingEntity target,
                                                         double currentSpeed) {
        ProjectilePhysicsProfile profile = projectile.getPhysicsProfile();
        double armor = target.getArmorValue() + armorToughness(target) * 0.5D;
        ImpactResults.Material body = new ImpactResults.Material(
                BODY_STRENGTH + armor * ARMOR_STRENGTH, BODY_DRAG, 0.0D, 1.0D, 1.0D);
        double realSpeed = projectile.realSpeed(currentSpeed);
        double kept = ImpactResolver.speedThrough(body, profile.mass(), profile.diameterOf(projectile),
                realSpeed, Math.max(0.25D, target.getBbWidth()));
        return kept < LEAST_FLIGHT_SPEED || realSpeed <= 0.0D ? 0.0D : currentSpeed * kept / realSpeed;
    }

    private static double armorToughness(LivingEntity target) {
        AttributeInstance toughness = target.getAttribute(Attributes.ARMOR_TOUGHNESS);
        return toughness == null ? 0.0 : toughness.getValue();
    }
}
