package me.mss1r.axiomata.blueprint.api.construction;

import me.mss1r.axiomata.blueprint.internal.construction.MaterialAllocation;
import net.minecraft.world.Container;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition.Material;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ConstructionWork {
    public enum Status {
        PROGRESSED,
        ADVANCED,
        DONE,
        MISSING_MATERIAL,
        CANNOT_END
    }

    /** {@code missing} is the material that stopped the stage, or null. */
    public record Result(Status status, @Nullable Material missing) {
        public boolean advanced() {
            return status == Status.ADVANCED;
        }

        public boolean worked() {
            return status == Status.ADVANCED || status == Status.PROGRESSED;
        }
    }

    private static final Result DONE = new Result(Status.DONE, null);

    private ConstructionWork() {
    }
    /** A null material source means free construction, currently used by creative players. */
    public static Result strike(UnderConstruction machine, Container materials) {
        BuildProgress progress = machine.buildProgress();
        BlueprintConstructionPlan.Stage stage = progress.currentStage();
        if (stage == null) {
            return DONE;
        }

        if (!progress.materialsCommitted() && materials != null) {
            Map<Material, Integer> missing = MaterialAllocation.shortfall(slots(materials), stage.materials());
            if (!missing.isEmpty()) {
                return new Result(Status.MISSING_MATERIAL, missing.keySet().iterator().next());
            }
        }

        if (!progress.materialsCommitted()) {
            // Commit the whole stage on its first hit and persist that fact. Reloading halfway
            // through a stage must not consume the same parts again.
            progress.commitMaterials(materials != null ? take(materials, stage.materials()) : List.of());
        }

        if (!progress.strike()) {
            machine.onBuildProgressChanged();
            return new Result(Status.PROGRESSED, null);
        }
        if (progress.complete()) {
            machine.applyBuiltData(progress.builtData());
        }
        machine.onBuildProgressChanged();
        return new Result(Status.ADVANCED, null);
    }

    /**
     * Ends an extendable build at its current stage and refunds materials already committed to that stage. A null
     * container skips the refund, as for creative players.
     */
    public static Result endHere(UnderConstruction machine, Container materials) {
        BuildProgress progress = machine.buildProgress();
        if (!progress.canEndHere()) {
            return new Result(Status.CANNOT_END, null);
        }
        if (progress.currentStage() != null && progress.materialsCommitted() && materials != null) {
            progress.currentStageRefund().forEach(stack -> give(materials, stack));
        }
        progress.endHere();
        machine.applyBuiltData(progress.builtData());
        machine.onBuildProgressChanged();
        return DONE;
    }

    public static Result dismantle(UnderConstruction machine, Container materials) {
        BuildProgress progress = machine.buildProgress();
        BlueprintConstructionPlan.Stage current = progress.currentStage();
        if (current != null && progress.hasCurrentStageWork()) {
            List<ItemStack> refund = progress.materialsCommitted() ? progress.currentStageRefund() : List.of();
            progress.cancelCurrentStage();
            if (materials != null) {
                refund.forEach(stack -> give(materials, stack));
            }
            machine.onBuildProgressChanged();
            return new Result(Status.ADVANCED, null);
        }

        int undone = progress.dismantle();
        if (undone < 0) {
            return DONE;
        }
        BlueprintConstructionPlan.Stage stage = progress.plan().stage(undone);
        if (stage != null && materials != null) {
            for (Material material : stage.materials()) {
                give(materials, material.returnStack());
            }
        }
        machine.onBuildProgressChanged();
        return new Result(Status.ADVANCED, null);
    }

    private static List<ItemStack> slots(Container container) {
        List<ItemStack> stacks = new ArrayList<>(container.getContainerSize());
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            stacks.add(container.getItem(slot));
        }
        return stacks;
    }

    /** Takes the materials and returns the items taken for them. */
    private static List<ItemStack> take(Container container, List<Material> materials) {
        List<ItemStack> stacks = slots(container);
        List<ItemStack> before = stacks.stream().map(ItemStack::copy).toList();
        MaterialAllocation.take(stacks, materials);
        List<ItemStack> taken = new ArrayList<>();
        for (int slot = 0; slot < stacks.size(); slot++) {
            int count = before.get(slot).getCount() - stacks.get(slot).getCount();
            if (count > 0) {
                taken.add(before.get(slot).copyWithCount(count));
            }
            if (!before.get(slot).isEmpty() && stacks.get(slot).isEmpty()) {
                container.setItem(slot, ItemStack.EMPTY);
            }
        }
        container.setChanged();
        return taken;
    }

    private static void give(Container container, ItemStack material) {
        for (int slot = 0; slot < container.getContainerSize() && !material.isEmpty(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (
                    //? if forge {
                    /*ItemStack.isSameItemSameTags(stack, material)
                    *///?} else {
                    ItemStack.isSameItemSameComponents(stack, material)
                    //?}
                    && stack.getCount() < stack.getMaxStackSize()) {
                int moved = Math.min(material.getCount(), stack.getMaxStackSize() - stack.getCount());
                stack.grow(moved);
                material.shrink(moved);
            }
        }
        for (int slot = 0; slot < container.getContainerSize() && !material.isEmpty(); slot++) {
            if (container.getItem(slot).isEmpty()) {
                container.setItem(slot, material.split(material.getCount()));
            }
        }
        container.setChanged();
    }
}
