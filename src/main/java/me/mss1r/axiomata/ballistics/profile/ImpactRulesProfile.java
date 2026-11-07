package me.mss1r.axiomata.ballistics.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.mss1r.axiomata.ballistics.impact.DamageScale;
import java.util.Optional;

/** Global gameplay scale and explicit geometry approximations for entities without authored impact data. */
public record ImpactRulesProfile(double secondaryReferenceEnergyJ, double secondaryReferenceDamageHp, double secondaryDamageExponent,
                                 double bodyReferenceMassKg, double bodyReferenceBoxVolumeM3,
                                 double fallbackArmorThicknessPerPointM, ImpactMaterialProfile bodyMaterial) {
    public static final ImpactRulesProfile DEFAULT = new ImpactRulesProfile(400, 20, 0.5, 70, 0.648, 0.0003,
            ImpactMaterialProfile.FLESH);
    public static final Codec<ImpactRulesProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("secondaryReferenceEnergyJ").forGetter(ImpactRulesProfile::secondaryReferenceEnergyJ),
            Codec.DOUBLE.fieldOf("secondaryReferenceDamageHp").forGetter(ImpactRulesProfile::secondaryReferenceDamageHp),
            Codec.DOUBLE.fieldOf("secondaryDamageExponent").forGetter(ImpactRulesProfile::secondaryDamageExponent),
            Codec.DOUBLE.fieldOf("bodyReferenceMassKg").forGetter(ImpactRulesProfile::bodyReferenceMassKg),
            Codec.DOUBLE.fieldOf("bodyReferenceBoxVolumeM3").forGetter(ImpactRulesProfile::bodyReferenceBoxVolumeM3),
            Codec.DOUBLE.fieldOf("fallbackArmorThicknessPerPointM").forGetter(ImpactRulesProfile::fallbackArmorThicknessPerPointM),
            ImpactMaterialProfile.CODEC.optionalFieldOf("bodyMaterial", ImpactMaterialProfile.FLESH).forGetter(ImpactRulesProfile::bodyMaterial)
    ).apply(instance, ImpactRulesProfile::new));

    public Optional<String> validationError() {
        for (double value : new double[]{secondaryReferenceEnergyJ, secondaryReferenceDamageHp, secondaryDamageExponent,
                bodyReferenceMassKg, bodyReferenceBoxVolumeM3, fallbackArmorThicknessPerPointM}) {
            if (!(value > 0) || !Double.isFinite(value)) return Optional.of("impact rules must be finite and positive");
        }
        return bodyMaterial.validationError();
    }

    public DamageScale secondaryDamage() { return new DamageScale(secondaryReferenceEnergyJ, secondaryReferenceDamageHp, secondaryDamageExponent); }
}
