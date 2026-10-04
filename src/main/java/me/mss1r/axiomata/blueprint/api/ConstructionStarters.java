package me.mss1r.axiomata.blueprint.api;

import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.item.BlueprintItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
//? if neoforge {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
//?}

import java.util.HashMap;
import java.util.Map;

/**
 * Items that start a build in place of a drawn blueprint. Such an item is the first stages of what it starts, made
 * elsewhere: held with a construction hammer it opens the build as a blueprint would, and the build begins with those
 * stages standing.
 */
public final class ConstructionStarters {
    private ConstructionStarters() {
    }

    /** The blueprint an item starts as a starter, or null when it starts none. */
    @Nullable
    public static String definitionFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for (Map.Entry<String, BlueprintDefinition> entry : BlueprintDefinitions.allById().entrySet()) {
            BlueprintDefinition.Starter starter = entry.getValue().starter();
            if (starter != null && itemId.equals(starter.item().toString())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** The blueprint an item held in a blueprint's place stands for: a drawn blueprint's own, or the one it starts. */
    @Nullable
    public static String definitionOf(ItemStack stack) {
        String drawn = BlueprintItem.getRecipeId(stack);
        return drawn != null ? drawn : definitionFor(stack);
    }

    public static boolean isStarter(ItemStack stack) {
        return BlueprintItem.getRecipeId(stack) == null && definitionFor(stack) != null;
    }

    /**
     * How many leading stages a starter already is: the ones it stands for, and after them each stage whose additions
     * its own data already holds, so a finished build taken up again carries on from where it stopped.
     */
    public static int builtStages(BlueprintDefinition definition, ItemStack starter) {
        if (definition.starter() == null) {
            return 0;
        }
        int stages = definition.stageCount();
        int built = Math.max(0, Math.min(definition.starter().builtStages(), stages));
        Map<String, Integer> added = new HashMap<>();
        for (int index = 0; index < built; index++) {
            add(added, definition.stages().get(index));
        }
        CompoundTag data = customData(starter);
        while (built < stages) {
            BlueprintDefinition.Stage next = definition.stages().get(built);
            if (next.adds().isEmpty() || !holds(data, added, next.adds())) {
                break;
            }
            add(added, next);
            built++;
        }
        return built;
    }

    private static boolean holds(CompoundTag data, Map<String, Integer> added, Map<String, Integer> adds) {
        for (Map.Entry<String, Integer> entry : adds.entrySet()) {
            if (added.getOrDefault(entry.getKey(), 0) + entry.getValue() > data.getInt(entry.getKey())) {
                return false;
            }
        }
        return true;
    }

    private static void add(Map<String, Integer> added, BlueprintDefinition.Stage stage) {
        stage.adds().forEach((key, value) -> added.merge(key, value, Integer::sum));
    }

    private static CompoundTag customData(ItemStack stack) {
        //? if forge {
        /*CompoundTag tag = stack.getTag();
        return tag == null ? new CompoundTag() : tag;
        *///?} else {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        //?}
    }
}
