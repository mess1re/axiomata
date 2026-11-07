package me.mss1r.axiomata.ballistics;

import me.mss1r.axiomata.ballistics.impact.BackedImpact;
import me.mss1r.axiomata.ballistics.impact.LayeredImpact;
import me.mss1r.axiomata.ballistics.profile.ArmorImpactProfile;
import me.mss1r.axiomata.ballistics.profile.ImpactMaterialProfile;
import me.mss1r.axiomata.ballistics.profile.ImpactProfiles;
import me.mss1r.axiomata.collision.CollidableStructure;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Passage and impulse are physical; work-to-HP here is used only for secondary fragments and pressure. */
public final class ProjectilePhysics {
    private ProjectilePhysics() {}
    public record Hit(double exitSpeedMps, boolean passedThrough, double workJ,
                      Vec3 targetVelocityMps, Vec3 exitPosition) {}

    public static Hit resolve(LayeredImpact.Shot shot, Vec3 velocityMps, Vec3 contact, LivingEntity target) {
        return resolve(shot, velocityMps, contact, target, target);
    }

    public static Hit resolve(LayeredImpact.Shot shot, Vec3 velocityMps, Vec3 contact, LivingEntity target, Entity struckGeometry) {
        double speed = velocityMps.length();
        if (!(speed > 0)) return new Hit(0, false, 0, target.getDeltaMovement().scale(20), contact);
        Vec3 direction = velocityMps.scale(1 / speed);
        var rules = ImpactProfiles.rules();
        double boxVolume = target.getBbWidth() * target.getBbWidth() * target.getBbHeight();
        StructuralImpactTarget structure = target instanceof StructuralImpactTarget value ? value : null;
        var material = structure == null ? rules.bodyMaterial() : structure.impactMaterial(contact);
        double mass = structure == null ? rules.bodyReferenceMassKg() * boxVolume / rules.bodyReferenceBoxVolumeM3()
                : structure.impactMassKg(contact);
        mass = Math.max(1e-6, mass);
        List<Span> spans = spans(struckGeometry, contact, direction);
        double length = spans.stream().mapToDouble(span -> span.end() - span.start()).sum();
        if (!(length > 0)) return new Hit(speed, true, 0, target.getDeltaMovement().scale(20), contact);
        Vec3 initialTargetVelocity = target.getDeltaMovement().scale(20);
        double targetSpeed = initialTargetVelocity.dot(direction);
        var body = new LayeredImpact.Layer(length, material.resistance());
        double bodyWork, kept, bodySpeed;
        boolean passed;
        ArmorImpactProfile armor = structure == null ? armorAt(target, contact) : null;
        if (armor != null) {
            Vec3 normal = entryNormal(target.getBoundingBox(), contact);
            var result = BackedImpact.resolve(shot, speed, armor.facing(Math.abs(direction.dot(normal))), mass, targetSpeed, body);
            bodyWork = result.bodyWorkJ();
            kept = result.projectileVelocityMps();
            bodySpeed = result.bodyVelocityMps();
            passed = result.passedThrough();
        } else {
            var result = LayeredImpact.resolve(shot, speed, mass, targetSpeed, List.of(body));
            bodyWork = result.workJ();
            kept = result.projectileVelocityMps();
            bodySpeed = result.targetVelocityMps();
            passed = result.passedThrough();
        }
        double end = spans.get(spans.size() - 1).end();
        return new Hit(Math.max(0, kept), passed, bodyWork,
                initialTargetVelocity.add(direction.scale(bodySpeed - targetSpeed)), contact.add(direction.scale(end + 1e-4)));
    }

    public static double secondaryDamageForWork(LivingEntity target, Vec3 at, double workJ) {
        return ImpactProfiles.rules().secondaryDamage().damage(workJ);
    }

    private static ArmorImpactProfile armorAt(LivingEntity target, Vec3 contact) {
        double height = (contact.y - target.getY()) / Math.max(1e-6, target.getBbHeight());
        EquipmentSlot slot = height > 0.75 ? EquipmentSlot.HEAD : height > 0.4 ? EquipmentSlot.CHEST
                : height > 0.1 ? EquipmentSlot.LEGS : EquipmentSlot.FEET;
        var stack = target.getItemBySlot(slot);
        var known = ImpactProfiles.ARMOR.snapshot().get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (known != null) return known;
        // Modded equipment and innate armor have no SI geometry. This explicit global fallback is replaceable by data.
        int equippedDefense = 0;
        for (var equipped : target.getArmorSlots()) if (equipped.getItem() instanceof ArmorItem item) equippedDefense += item.getDefense();
        int innateDefense = Math.max(0, target.getArmorValue() - equippedDefense);
        int defense = stack.getItem() instanceof ArmorItem item ? item.getDefense() + innateDefense : innateDefense;
        if (defense <= 0) return null;
        double area = target.getBbWidth() * target.getBbHeight() * (slot == EquipmentSlot.CHEST ? 0.4 : 0.2);
        return new ArmorImpactProfile(ImpactMaterialProfile.IRON, Math.max(1e-6, area),
                defense * ImpactProfiles.rules().fallbackArmorThicknessPerPointM());
    }

    private record Span(double start, double end) {}

    /** Solid ray intervals are merged, so overlapping authored cubes do not spend resistance work twice. */
    private static List<Span> spans(Entity target, Vec3 contact, Vec3 direction) {
        var spans = new ArrayList<Span>();
        if (target instanceof CollidableStructure structure) {
            Vec3 origin = structure.collisionTransform().toLocal(contact);
            Vec3 end = structure.collisionTransform().toLocal(contact.add(direction));
            for (var group : structure.collisionGroups()) for (var part : group.parts()) {
                Vec3 local = group.toPart(part, origin);
                Span span = interval(part.box(), local, group.toPart(part, end).subtract(local));
                if (span != null) spans.add(span);
            }
        } else {
            Span span = interval(target.getBoundingBox(), contact, direction);
            if (span != null) spans.add(span);
        }
        spans.sort(Comparator.comparingDouble(Span::start));
        var merged = new ArrayList<Span>();
        for (Span span : spans) {
            if (merged.isEmpty() || span.start() > merged.get(merged.size() - 1).end()) merged.add(span);
            else {
                Span previous = merged.remove(merged.size() - 1);
                merged.add(new Span(previous.start(), Math.max(previous.end(), span.end())));
            }
        }
        return merged;
    }

    private static Span interval(AABB box, Vec3 origin, Vec3 direction) {
        double near = 0, far = Double.POSITIVE_INFINITY;
        double[] p = {origin.x, origin.y, origin.z}, d = {direction.x, direction.y, direction.z};
        double[] min = {box.minX, box.minY, box.minZ}, max = {box.maxX, box.maxY, box.maxZ};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1e-12) { if (p[i] < min[i] || p[i] > max[i]) return null; }
            else {
                double a = (min[i] - p[i]) / d[i], b = (max[i] - p[i]) / d[i];
                near = Math.max(near, Math.min(a, b));
                far = Math.min(far, Math.max(a, b));
            }
        }
        return far > near && Double.isFinite(far) ? new Span(near, far) : null;
    }

    private static Vec3 entryNormal(AABB box, Vec3 at) {
        double[] distances = {Math.abs(at.x - box.minX), Math.abs(at.x - box.maxX), Math.abs(at.y - box.minY),
                Math.abs(at.y - box.maxY), Math.abs(at.z - box.minZ), Math.abs(at.z - box.maxZ)};
        int index = 0;
        for (int i = 1; i < distances.length; i++) if (distances[i] < distances[index]) index = i;
        return switch (index) {
            case 0 -> new Vec3(-1, 0, 0); case 1 -> new Vec3(1, 0, 0); case 2 -> new Vec3(0, -1, 0);
            case 3 -> new Vec3(0, 1, 0); case 4 -> new Vec3(0, 0, -1); default -> new Vec3(0, 0, 1);
        };
    }
    public static float damageWithArmorPiercing(double armorPiercing, float rawDamage,
                                                 LivingEntity target) {
        float damage = Math.max(0.0F, rawDamage);
        float piercing = Mth.clamp((float) armorPiercing, 0.0F, 1.0F);
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
        //? if <1.21 {
        /*return CombatRules.getDamageAfterAbsorb(damage, armor, toughness);
        *///?} else {
        return CombatRules.getDamageAfterAbsorb(
                target, damage, target.damageSources().generic(), armor, toughness);
        //?}
    }


    private static double armorToughness(LivingEntity target) {
        AttributeInstance toughness = target.getAttribute(Attributes.ARMOR_TOUGHNESS);
        return toughness == null ? 0.0 : toughness.getValue();
    }

}
