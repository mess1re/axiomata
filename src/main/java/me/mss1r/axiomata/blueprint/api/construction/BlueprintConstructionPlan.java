package me.mss1r.axiomata.blueprint.api.construction;

import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.config.BlueprintServerConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BlueprintConstructionPlan(List<Stage> stages) {
    public record Stage(String section, List<ItemStack> materials, int hits) {
    }

    public static BlueprintConstructionPlan of(BlueprintDefinition recipe, BuildQuality quality) {
        List<Stage> stages = new ArrayList<>();
        if (recipe != null && recipe.construction != null && !recipe.construction.isEmpty()) {
            Map<String, Integer> allocatedBaseMaterials = new LinkedHashMap<>();
            for (BlueprintDefinition.StageSpec spec : recipe.construction) {
                Stage stage = stageOf(recipe, spec, quality, allocatedBaseMaterials);
                if (stage != null) {
                    stages.add(stage);
                }
            }
        }
        if (stages.isEmpty()) {
            stages.add(new Stage("", List.of(), quality.hitsFor(hitsFor(0))));
        }
        return new BlueprintConstructionPlan(Collections.unmodifiableList(stages));
    }

    private static Stage stageOf(BlueprintDefinition recipe, BlueprintDefinition.StageSpec spec,
                                 BuildQuality quality, Map<String, Integer> allocatedBaseMaterials) {
        if (spec == null || spec.section == null || spec.section.isBlank()) {
            return null;
        }
        List<ItemStack> materials = new ArrayList<>();
        int baseCount = 0;
        for (BlueprintDefinition.StagePartSpec part : spec.parts()) {
            BlueprintDefinition.IngredientSpec ingredient = part.key == null ? null : recipe.key.get(part.key);
            if (ingredient == null) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(ingredient.item));
            if (item == null) {
                continue;
            }
            int needed = part.count > 0 ? part.count : Math.max(1, ingredient.count);
            baseCount += needed;
            // Apply the quality multiplier to the running total. Rounding every stage on its own
            // overcharges ingredients that are split across several stages.
            int allocatedBefore = allocatedBaseMaterials.getOrDefault(part.key, 0);
            int allocatedAfter = allocatedBefore + needed;
            int adjustedBefore = quality.materialsFor(allocatedBefore);
            int adjustedAfter = quality.materialsFor(allocatedAfter);
            allocatedBaseMaterials.put(part.key, allocatedAfter);
            materials.add(new ItemStack(item, adjustedAfter - adjustedBefore));
        }
        // Work is based on the actual part count. Poor quality already costs extra material; using
        // that inflated count here would charge the same penalty twice.
        int base = spec.hits > 0 ? spec.hits : hitsFor(baseCount);
        int hits = quality.hitsFor(base);
        return new Stage(spec.section, List.copyOf(materials), hits);
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

    me.mss1r.axiomata.construction.ConstructionPlan<ItemStack> core() {
        return new me.mss1r.axiomata.construction.ConstructionPlan<>(
                stages.stream()
                        .map(stage -> new me.mss1r.axiomata.construction.ConstructionPlan.Stage<>(
                                stage.section(), stage.materials(), stage.hits()))
                        .toList());
    }
}
