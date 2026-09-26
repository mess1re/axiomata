package me.mss1r.axiomata.blueprint.api;

import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class SchematicItemRegistry {
    private SchematicItemRegistry() {
    }

    public static Set<Item> getItems() {
        Set<Item> items = new LinkedHashSet<>();
        BlueprintDefinitions.all().forEach(recipe -> recipe.key.values().forEach(spec -> {
            ResourceLocation id = ResourceLocation.tryParse(spec.item);
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                items.add(BuiltInRegistries.ITEM.get(id));
            }
        }));
        return Collections.unmodifiableSet(items);
    }

    public static boolean contains(Item item) {
        return getItems().contains(item);
    }
}
