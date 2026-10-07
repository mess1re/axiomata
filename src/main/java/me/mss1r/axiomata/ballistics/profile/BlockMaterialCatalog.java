package me.mss1r.axiomata.ballistics.profile;

import me.mss1r.axiomata.data.profile.ProfileCatalog;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class BlockMaterialCatalog {
    private final ProfileCatalog<BlockMaterialProfile> profiles = new ProfileCatalog<>(
            new BlockMaterialProfile(Optional.empty(), Optional.empty(), 0,
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
    private volatile List<Rule> rules = List.of();

    public BlockMaterialCatalog() {
        profiles.onPublish(() -> rules = profiles.snapshot().entrySet().stream()
                .map(entry -> new Rule(entry.getKey(), entry.getValue()))
                .sorted(Comparator.<Rule>comparingInt(rule -> rule.profile().block().isPresent() ? 0 : 1)
                        .thenComparing(Comparator.comparingInt((Rule rule) -> rule.profile().priority()).reversed())
                        .thenComparing(rule -> rule.id().toString()))
                .toList());
    }

    public ProfileCatalog<BlockMaterialProfile> profiles() {
        return profiles;
    }

    public Optional<BlockMaterialProfile> forState(BlockState state) {
        ResourceLocation block = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        for (Rule rule : rules) {
            BlockMaterialProfile profile = rule.profile();
            if (profile.block().filter(block::equals).isPresent()
                    || profile.tag().filter(tag -> state.is(TagKey.create(Registries.BLOCK, tag))).isPresent()) {
                return Optional.of(profile);
            }
        }
        return Optional.empty();
    }

    public double resistance(BlockState state) {
        return forState(state).flatMap(BlockMaterialProfile::projectileResistance)
                .orElseGet(() -> (double) state.getBlock().getExplosionResistance());
    }

    public void reset() {
        rules = List.of();
        profiles.reset();
    }

    private record Rule(ResourceLocation id, BlockMaterialProfile profile) {
    }
}
