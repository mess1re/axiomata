package me.mss1r.axiomata.blueprint.api;

import me.mss1r.axiomata.blueprint.BlueprintModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class BlueprintTags {
    public static final TagKey<Item> CONSTRUCTION_HAMMERS = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(BlueprintModule.MOD_ID, "construction_hammers")
    );

    private BlueprintTags() {
    }
}
