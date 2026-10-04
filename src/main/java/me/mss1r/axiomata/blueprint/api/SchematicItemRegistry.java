package me.mss1r.axiomata.blueprint.api;

import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class SchematicItemRegistry {
    private SchematicItemRegistry() {
    }

    public static Set<Item> getItems() {
        Set<Item> items = new LinkedHashSet<>();
        BlueprintDefinitions.all().forEach(recipe -> recipe.totals().forEach(material -> {
            if (material.item() != null && BuiltInRegistries.ITEM.containsKey(material.item())) {
                items.add(BuiltInRegistries.ITEM.get(material.item()));
            } else if (material.tag() != null) {
                BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, material.tag()))
                        .ifPresent(tag -> tag.forEach(holder -> items.add(holder.value())));
            }
        }));
        return Collections.unmodifiableSet(items);
    }

    public static boolean contains(Item item) {
        return getItems().contains(item);
    }
}
