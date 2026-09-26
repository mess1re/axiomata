package me.mss1r.axiomata.blueprint.api.construction;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class ConstructionWork {
    public enum Status {
        PROGRESSED,
        ADVANCED,
        DONE,
        MISSING_MATERIAL
    }

    public record Result(Status status, ItemStack missing) {
        public boolean advanced() {
            return status == Status.ADVANCED;
        }

        public boolean worked() {
            return status == Status.ADVANCED || status == Status.PROGRESSED;
        }
    }

    private static final Result DONE = new Result(Status.DONE, ItemStack.EMPTY);

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
            for (ItemStack material : stage.materials()) {
                if (!material.isEmpty() && count(materials, material) < material.getCount()) {
                    return new Result(Status.MISSING_MATERIAL, material);
                }
            }
        }

        if (!progress.materialsCommitted()) {
            // Commit the whole stage on its first hit and persist that fact. Reloading halfway
            // through a stage must not consume the same parts again.
            if (materials != null) {
                for (ItemStack material : stage.materials()) {
                    take(materials, material);
                }
            }
            progress.commitMaterials();
        }

        if (!progress.strike()) {
            machine.onBuildProgressChanged();
            return new Result(Status.PROGRESSED, ItemStack.EMPTY);
        }
        machine.onBuildProgressChanged();
        return new Result(Status.ADVANCED, ItemStack.EMPTY);
    }

    public static Result dismantle(UnderConstruction machine, Container materials) {
        BuildProgress progress = machine.buildProgress();
        BlueprintConstructionPlan.Stage current = progress.currentStage();
        if (current != null && progress.hasCurrentStageWork()) {
            boolean refund = progress.materialsCommitted();
            progress.cancelCurrentStage();
            if (refund && materials != null) {
                for (ItemStack material : current.materials()) {
                    give(materials, material.copy());
                }
            }
            machine.onBuildProgressChanged();
            return new Result(Status.ADVANCED, ItemStack.EMPTY);
        }

        int undone = progress.dismantle();
        if (undone < 0) {
            return DONE;
        }
        BlueprintConstructionPlan.Stage stage = progress.plan().stage(undone);
        if (stage != null && materials != null) {
            for (ItemStack material : stage.materials()) {
                give(materials, material.copy());
            }
        }
        machine.onBuildProgressChanged();
        return new Result(Status.ADVANCED, ItemStack.EMPTY);
    }

    private static int count(Container container, ItemStack material) {
        int found = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.getItem() == material.getItem()) {
                found += stack.getCount();
            }
        }
        return found;
    }

    private static void take(Container container, ItemStack material) {
        int remaining = material.getCount();
        for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.getItem() != material.getItem()) {
                continue;
            }
            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            remaining -= taken;
            if (stack.isEmpty()) {
                container.setItem(slot, ItemStack.EMPTY);
            }
        }
        container.setChanged();
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
