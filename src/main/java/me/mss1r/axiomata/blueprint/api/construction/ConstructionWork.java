package me.mss1r.axiomata.blueprint.api.construction;

import net.minecraft.world.Container;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition.Material;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ConstructionWork {
    public enum Status {
        PROGRESSED,
        ADVANCED,
        DONE,
        MISSING_MATERIAL,
        CANNOT_END
    }

    /** {@code missing} is the material a stage lacks, when that is what stopped it. */
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
            for (Material material : stage.materials()) {
                if (count(materials, material) < material.count()) {
                    return new Result(Status.MISSING_MATERIAL, material);
                }
            }
        }

        if (!progress.materialsCommitted()) {
            // Commit the whole stage on its first hit and persist that fact. Reloading halfway
            // through a stage must not consume the same parts again.
            if (materials != null) {
                for (Material material : stage.materials()) {
                    take(materials, material);
                }
            }
            progress.commitMaterials();
        }

        if (!progress.strike()) {
            machine.onBuildProgressChanged();
            return new Result(Status.PROGRESSED, null);
        }
        machine.onBuildProgressChanged();
        return new Result(Status.ADVANCED, null);
    }

    /**
     * Ends an extendable build at the stage it has reached, handing back whatever the unfinished stage had already
     * taken. A null material source takes nothing back, as for creative players.
     */
    public static Result endHere(UnderConstruction machine, Container materials) {
        BuildProgress progress = machine.buildProgress();
        if (!progress.canEndHere()) {
            return new Result(Status.CANNOT_END, null);
        }
        BlueprintConstructionPlan.Stage current = progress.currentStage();
        if (current != null && progress.materialsCommitted() && materials != null) {
            for (Material material : current.materials()) {
                give(materials, material.displayStack());
            }
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
            boolean refund = progress.materialsCommitted();
            progress.cancelCurrentStage();
            if (refund && materials != null) {
                for (Material material : current.materials()) {
                    give(materials, material.displayStack());
                }
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
                give(materials, material.displayStack());
            }
        }
        machine.onBuildProgressChanged();
        return new Result(Status.ADVANCED, null);
    }

    private static int count(Container container, Material material) {
        int found = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (material.matches(stack)) {
                found += stack.getCount();
            }
        }
        return found;
    }

    private static void take(Container container, Material material) {
        int remaining = material.count();
        for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = container.getItem(slot);
            if (!material.matches(stack)) {
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

    /** Hands back a material; one taken by a tag comes back as the item that shows that tag. */
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
