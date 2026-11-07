package me.mss1r.axiomata.ballistics.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.mss1r.axiomata.ballistics.impact.BackedImpact;
import me.mss1r.axiomata.ballistics.impact.LayeredImpact;
import java.util.Optional;

/** Effective facing panel, keyed by item ID in definitions/armor_impacts. Thickness is along its surface normal. */
public record ArmorImpactProfile(ImpactMaterialProfile material, double areaM2, double thicknessM) {
    public static final ArmorImpactProfile DEFAULT = new ArmorImpactProfile(ImpactMaterialProfile.IRON, 0.2, 0.002);
    public static final Codec<ArmorImpactProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ImpactMaterialProfile.CODEC.fieldOf("material").forGetter(ArmorImpactProfile::material),
            Codec.DOUBLE.fieldOf("areaM2").forGetter(ArmorImpactProfile::areaM2),
            Codec.DOUBLE.fieldOf("thicknessM").forGetter(ArmorImpactProfile::thicknessM)
    ).apply(instance, ArmorImpactProfile::new));

    public Optional<String> validationError() {
        if (!(areaM2 > 0) || !Double.isFinite(areaM2) || !(thicknessM > 0) || !Double.isFinite(thicknessM)) {
            return Optional.of("armor area and thickness must be finite and positive");
        }
        return material.validationError();
    }

    public BackedImpact.Facing facing(double incidenceCosine) {
        return new BackedImpact.Facing(material.density() * areaM2 * thicknessM, areaM2,
                new LayeredImpact.Layer(thicknessM / Math.max(1e-6, incidenceCosine), material.resistance()));
    }
}
