package me.mss1r.axiomata.blueprint.internal.construction;

import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition.Material;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Takes several materials out of one store together. Each item counts towards one material only, and materials named
 * by an item are served before those named by a tag, so a tag does not use up what only that item could fill.
 */
public final class MaterialAllocation {
    private MaterialAllocation() {
    }

    /** How many of each material the stacks fall short of, in the order given; empty when they hold everything. */
    public static Map<Material, Integer> shortfall(List<ItemStack> stacks, List<Material> materials) {
        int[] left = counts(stacks);
        Map<Material, Integer> drawn = new LinkedHashMap<>();
        for (Material material : servingOrder(materials)) {
            drawn.put(material, draw(stacks, left, material));
        }
        Map<Material, Integer> missing = new LinkedHashMap<>();
        for (Material material : materials) {
            int lacking = material.count() - drawn.getOrDefault(material, 0);
            if (lacking > 0) {
                missing.put(material, lacking);
            }
        }
        return missing;
    }

    /** Takes the materials out of the stacks, as {@link #shortfall} counts them. */
    public static void take(List<ItemStack> stacks, List<Material> materials) {
        int[] left = counts(stacks);
        for (Material material : servingOrder(materials)) {
            draw(stacks, left, material);
        }
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            stack.shrink(stack.getCount() - left[slot]);
        }
    }

    private static int[] counts(List<ItemStack> stacks) {
        int[] counts = new int[stacks.size()];
        for (int slot = 0; slot < stacks.size(); slot++) {
            counts[slot] = stacks.get(slot).getCount();
        }
        return counts;
    }

    /** Sets aside as much of a material as is left in the stacks, and says how much that was. */
    private static int draw(List<ItemStack> stacks, int[] left, Material material) {
        int wanted = material.count();
        for (int slot = 0; slot < stacks.size() && wanted > 0; slot++) {
            if (left[slot] > 0 && material.matches(stacks.get(slot))) {
                int taken = Math.min(wanted, left[slot]);
                left[slot] -= taken;
                wanted -= taken;
            }
        }
        return material.count() - wanted;
    }

    private static List<Material> servingOrder(List<Material> materials) {
        List<Material> ordered = new ArrayList<>(materials);
        ordered.sort(Comparator.comparing(material -> material.item() == null));
        return ordered;
    }
}
