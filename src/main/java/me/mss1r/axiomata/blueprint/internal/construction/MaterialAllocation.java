package me.mss1r.axiomata.blueprint.internal.construction;

import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition.Material;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Takes several materials from one inventory at once. Each stack counts toward one material only, and item materials
 * are matched before tag materials so a tag cannot use up items an exact material needs.
 */
public final class MaterialAllocation {
    private MaterialAllocation() {
    }

    /** Missing count per material, in the given order. Empty when everything is available. */
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

    /** Removes the materials, allocated the same way as {@link #shortfall}. */
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

    /** Reserves up to the material's count from {@code left} and returns how much was reserved. */
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
