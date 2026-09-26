package me.mss1r.axiomata.blueprint.api;

import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.item.BlueprintItem;
import me.mss1r.axiomata.blueprint.registry.BlueprintItems;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class BlueprintStacks {
    private BlueprintStacks() {
    }

    public static boolean isBlueprint(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == BlueprintItems.BLUEPRINT.get();
    }

    @Nullable
    public static String definitionId(ItemStack stack) {
        return BlueprintItem.getRecipeId(stack);
    }

    @Nullable
    public static BlueprintDefinition definition(ItemStack stack) {
        String id = definitionId(stack);
        return id == null ? null : BlueprintDefinitions.get(id);
    }

    public static ItemStack create(String definitionId) {
        BlueprintDefinition definition = BlueprintDefinitions.get(definitionId);
        return definition == null ? ItemStack.EMPTY : BlueprintItem.create(definition, definitionId);
    }

    public static ItemStack createResult(BlueprintDefinition definition) {
        return BlueprintItem.createResultStack(definition);
    }

    public static ItemStack createResult(String definitionId) {
        BlueprintDefinition definition = BlueprintDefinitions.get(definitionId);
        return definition == null ? ItemStack.EMPTY : createResult(definition);
    }

    public static ItemStack constructionHammer() {
        return new ItemStack(BlueprintItems.CONSTRUCTION_HAMMER.get());
    }
}
