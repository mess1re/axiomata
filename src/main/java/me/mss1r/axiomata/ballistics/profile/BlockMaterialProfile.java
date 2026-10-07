package me.mss1r.axiomata.ballistics.profile;

import me.mss1r.axiomata.data.profile.ProfileValidation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/** Material values for a whole solid block; its collision shape and porosity still apply. */
public record BlockMaterialProfile(Optional<ResourceLocation> block, Optional<ResourceLocation> tag, int priority,
                                   Optional<Double> strength, Optional<Double> drag,
                                   Optional<Double> fractureEnergy, Optional<Double> projectileResistance) {
    /** Block tag ID, with or without the leading {@code #}. */
    private static final Codec<ResourceLocation> TAG_ID = Codec.STRING.comapFlatMap(text -> {
        ResourceLocation id = ResourceLocation.tryParse(text.startsWith("#") ? text.substring(1) : text);
        return id == null ? DataResult.error(() -> "not a tag id: " + text) : DataResult.success(id);
    }, id -> "#" + id);
    public static final Codec<BlockMaterialProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("block").forGetter(BlockMaterialProfile::block),
            TAG_ID.optionalFieldOf("tag").forGetter(BlockMaterialProfile::tag),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(BlockMaterialProfile::priority),
            Codec.DOUBLE.optionalFieldOf("strength").forGetter(BlockMaterialProfile::strength),
            Codec.DOUBLE.optionalFieldOf("drag").forGetter(BlockMaterialProfile::drag),
            Codec.DOUBLE.optionalFieldOf("fractureEnergy").forGetter(BlockMaterialProfile::fractureEnergy),
            Codec.DOUBLE.optionalFieldOf("projectileResistance").forGetter(BlockMaterialProfile::projectileResistance)
    ).apply(instance, BlockMaterialProfile::new));

    public Optional<String> validationError() {
        if (block.isPresent() == tag.isPresent()) {
            return Optional.of("set either block or tag, not both");
        }
        if (block.isPresent() && !BuiltInRegistries.BLOCK.containsKey(block.get())) {
            return Optional.of("unknown block " + block.get());
        }
        if (strength.isEmpty() && drag.isEmpty() && fractureEnergy.isEmpty() && projectileResistance.isEmpty()) {
            return Optional.of("set at least one material value");
        }
        var names = List.of("strength", "drag", "fractureEnergy");
        var values = List.of(strength, drag, fractureEnergy);
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).isPresent()) {
                double value = values.get(i).get();
                if (!(value > 0.0D) || !Double.isFinite(value)) {
                    return Optional.of(names.get(i) + " must be finite and greater than zero");
                }
            }
        }
        return projectileResistance.flatMap(value -> ProfileValidation.nonNegative("projectileResistance", value));
    }
}
