package me.mss1r.axiomata.ballistics.damage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Receiving structure's HP response, independent of physical penetration resistance. */
public record StructureDamageProfile(Map<String, Double> multipliers) {
    public static final Set<String> CATEGORIES = Set.of("bolt", "solid_projectile", "light_projectile",
            "fragment", "explosion", "melee", "impact", "fire", "other");
    public static final StructureDamageProfile DEFAULT = new StructureDamageProfile(Map.of());
    public static final Codec<StructureDamageProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("multipliers", Map.of())
                    .forGetter(StructureDamageProfile::multipliers)
    ).apply(instance, StructureDamageProfile::new));

    public StructureDamageProfile { multipliers = Map.copyOf(multipliers); }

    public double multiplier(String category) { return multipliers.getOrDefault(category, 1.0); }

    public Optional<String> validationError() {
        for (var entry : multipliers.entrySet()) {
            if (!CATEGORIES.contains(entry.getKey())) return Optional.of("unknown damage category: " + entry.getKey());
            if (!Double.isFinite(entry.getValue()) || entry.getValue() < 0)
                return Optional.of("multipliers." + entry.getKey() + " must be finite and nonnegative");
        }
        return Optional.empty();
    }

    public static Optional<String> formatError(com.google.gson.JsonElement json) {
        if (!json.isJsonObject()) return Optional.of("expected a structure damage profile object");
        return json.getAsJsonObject().keySet().stream().filter(key -> !key.equals("multipliers")).sorted().findFirst()
                .map(key -> "unknown structure damage field: " + key);
    }
}
