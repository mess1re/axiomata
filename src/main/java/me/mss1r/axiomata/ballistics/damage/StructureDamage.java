package me.mss1r.axiomata.ballistics.damage;

import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.ballistics.ProjectileCombat;
import me.mss1r.axiomata.data.profile.ProfileCatalog;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;

/** Call amount() once in the receiving entity's hurt path, after access checks and before applying HP damage. */
public final class StructureDamage {
    public static final ProfileCatalog<StructureDamageProfile> PROFILES = new ProfileCatalog<>(StructureDamageProfile.DEFAULT);
    public static final TagKey<DamageType> BOLTS = tag("bolts");
    public static final TagKey<DamageType> SOLID_PROJECTILES = tag("solid_projectiles");
    public static final TagKey<DamageType> LIGHT_PROJECTILES = tag("light_projectiles");
    public static final TagKey<DamageType> FRAGMENTS = tag("fragments");
    public static final TagKey<DamageType> MELEE = tag("melee");
    private StructureDamage() {}

    private static TagKey<DamageType> tag(String name) {
        return TagKey.create(Registries.DAMAGE_TYPE, ResourceIds.id("axiomata", name));
    }

    /** A single most-specific category is selected; broad vanilla tags do not multiply again. */
    public static String category(DamageSource source) {
        if (source.is(FRAGMENTS)) return "fragment";
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return "explosion";
        if (source.is(BOLTS)) return "bolt";
        if (source.is(SOLID_PROJECTILES)) return "solid_projectile";
        if (source.is(ProjectileCombat.PHYSICAL_IMPACT)) return "impact";
        if (source.is(DamageTypeTags.IS_FIRE)) return "fire";
        if (source.is(MELEE) || source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK)) return "melee";
        if (source.is(LIGHT_PROJECTILES) || source.is(DamageTypeTags.IS_PROJECTILE)) return "light_projectile";
        return "other";
    }

    public static StructureDamageProfile profile(Entity target) {
        return target instanceof StructureDamageTarget selection ? PROFILES.get(selection.structureDamageProfileId())
                : PROFILES.forEntity(target.getType());
    }

    public static float amount(Entity target, DamageSource source, float incoming) {
        return (float) Math.min(Float.MAX_VALUE, incoming * profile(target).multiplier(category(source)));
    }
}
