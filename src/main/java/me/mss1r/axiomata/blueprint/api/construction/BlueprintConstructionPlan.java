package me.mss1r.axiomata.blueprint.api.construction;

import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition.Material;
import me.mss1r.axiomata.blueprint.config.BlueprintServerConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BlueprintConstructionPlan(List<Stage> stages) {
    public static final TagKey<Item> QUALITY_COST_EXEMPT = TagKey.create(Registries.ITEM,
            ResourceLocation.tryParse("axiomata:quality_cost_exempt"));
    /** A stage with the quality multipliers applied. */
    public record Stage(String section, List<Material> materials, int hits) {
    }

    public static BlueprintConstructionPlan of(BlueprintDefinition recipe, BuildQuality quality) {
        List<Stage> stages = new ArrayList<>();
        if (recipe != null) {
            Map<String, Integer> allocatedBaseMaterials = new LinkedHashMap<>();
            for (BlueprintDefinition.Stage spec : recipe.stages()) {
                stages.add(stageOf(spec, quality, allocatedBaseMaterials));
            }
        }
        if (stages.isEmpty()) {
            stages.add(new Stage("", List.of(), quality.hitsFor(hitsFor(0))));
        }
        return new BlueprintConstructionPlan(Collections.unmodifiableList(stages));
    }

    private static Stage stageOf(BlueprintDefinition.Stage spec, BuildQuality quality,
                                 Map<String, Integer> allocatedBaseMaterials) {
        List<Material> materials = new ArrayList<>();
        int baseCount = 0;
        for (Material material : spec.materials()) {
            int needed = material.count();
            baseCount += needed;
            // Apply the quality multiplier to the running total. Rounding every stage on its own
            // overcharges materials that are split across several stages.
            int allocatedBefore = allocatedBaseMaterials.getOrDefault(material.key(), 0);
            int allocatedAfter = allocatedBefore + needed;
            allocatedBaseMaterials.put(material.key(), allocatedAfter);
            int taken = qualityCostExempt(material) ? needed
                    : quality.materialsFor(allocatedAfter) - quality.materialsFor(allocatedBefore);
            if (taken > 0) {
                materials.add(material.withCount(taken));
            }
        }
        // Work is based on the actual part count. Poor quality already costs extra material; using
        // that inflated count here would charge the same penalty twice.
        int base = spec.hits() > 0 ? spec.hits() : hitsFor(baseCount);
        return new Stage(spec.section(), List.copyOf(materials), quality.hitsFor(base));
    }

    private static boolean qualityCostExempt(Material material) {
        if (material.item() != null) {
            return BuiltInRegistries.ITEM.get(material.item()).builtInRegistryHolder().is(QUALITY_COST_EXEMPT);
        }
        return BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, material.tag()))
                .map(items -> items.size() > 0 && items.stream().allMatch(item -> item.is(QUALITY_COST_EXEMPT)))
                .orElse(false);
    }

    public static int hitsFor(int itemCount) {
        // Item count is the predictable default. A blueprint can still specify an exact value for
        // parts whose assembly cost is not represented well by stack size.
        int hits = (int) Math.ceil(itemCount * BlueprintServerConfig.getHitsPerItem());
        return Math.max(BlueprintServerConfig.getMinStageHits(),
                Math.min(BlueprintServerConfig.getMaxStageHits(), hits));
    }

    public int stageCount() {
        return stages.size();
    }

    public Stage stage(int index) {
        return index >= 0 && index < stages.size() ? stages.get(index) : null;
    }

    public int totalHits() {
        int total = 0;
        for (Stage stage : stages) {
            total += stage.hits();
        }
        return total;
    }

    me.mss1r.axiomata.construction.ConstructionPlan<Material> core() {
        return new me.mss1r.axiomata.construction.ConstructionPlan<>(
                stages.stream()
                        .map(stage -> new me.mss1r.axiomata.construction.ConstructionPlan.Stage<>(
                                stage.section(), stage.materials(), stage.hits()))
                        .toList());
    }
}
