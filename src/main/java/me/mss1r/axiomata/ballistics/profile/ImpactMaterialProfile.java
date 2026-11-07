package me.mss1r.axiomata.ballistics.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.mss1r.axiomata.ballistics.impact.Fragmentation;
import me.mss1r.axiomata.ballistics.impact.Penetration;

import java.util.Optional;

/** SI material coefficients for the impact approximation; values are gameplay calibrations, not certified armor data. */
public record ImpactMaterialProfile(double density, double strength, double drag, double fractureEnergy,
                                    double surfaceEnergy, double fragmentSize, double ejectionFraction) {
    public static final ImpactMaterialProfile IRON = new ImpactMaterialProfile(7800, 300e6, 7800, 5e6, 20_000, 0.005, 0.15);
    public static final ImpactMaterialProfile WOOD = new ImpactMaterialProfile(650, 40e6, 650, 20_000, 1000, 0.04, 0.2);
    public static final ImpactMaterialProfile STONE = new ImpactMaterialProfile(2600, 16.7e6, 625, 50_000, 200, 0.05, 0.15);
    public static final ImpactMaterialProfile CLAY = new ImpactMaterialProfile(2000, 5e6, 1000, 10_000, 100, 0.025, 0.15);
    public static final ImpactMaterialProfile FLESH = new ImpactMaterialProfile(1000, 300_000, 1000, 300_000, 10_000, 0.05, 0);
    public static final Codec<ImpactMaterialProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("density").forGetter(ImpactMaterialProfile::density),
            Codec.DOUBLE.fieldOf("strength").forGetter(ImpactMaterialProfile::strength),
            Codec.DOUBLE.fieldOf("drag").forGetter(ImpactMaterialProfile::drag),
            Codec.DOUBLE.fieldOf("fractureEnergy").forGetter(ImpactMaterialProfile::fractureEnergy),
            Codec.DOUBLE.fieldOf("surfaceEnergy").forGetter(ImpactMaterialProfile::surfaceEnergy),
            Codec.DOUBLE.fieldOf("fragmentSize").forGetter(ImpactMaterialProfile::fragmentSize),
            Codec.DOUBLE.fieldOf("ejectionFraction").forGetter(ImpactMaterialProfile::ejectionFraction)
    ).apply(instance, ImpactMaterialProfile::new));

    public Optional<String> validationError() {
        for (double value : new double[]{density, strength, drag, fractureEnergy, surfaceEnergy, fragmentSize}) {
            if (!(value > 0) || !Double.isFinite(value)) return Optional.of("material coefficients must be finite and positive");
        }
        if (!(ejectionFraction >= 0 && ejectionFraction <= 1)) return Optional.of("material.ejectionFraction must be from zero to one");
        return Optional.empty();
    }

    public Penetration.Material resistance() { return new Penetration.Material(strength, drag); }
    public Fragmentation.Material fragmentation() { return new Fragmentation.Material(density, surfaceEnergy, fragmentSize, ejectionFraction); }
}
