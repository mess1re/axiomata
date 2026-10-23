package me.mss1r.axiomata.blueprint.api.construction;

import me.mss1r.axiomata.blueprint.api.event.BlueprintBuiltEvent;
import me.mss1r.axiomata.blueprint.api.event.BlueprintEvents;
import me.mss1r.axiomata.blueprint.internal.construction.ConstructionSync;
import net.minecraft.world.Container;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition.Material;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
        MISSING_MATERIAL
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
        return strike(machine, materials, null);
    }

    /**
     * A blow by {@code worker}, who is named in {@link BlueprintEvents#BUILT} when the blow finishes the build.
     * A null material source means free construction, currently used by creative players.
     */
    public static Result strike(UnderConstruction machine, Container materials, @Nullable LivingEntity worker) {
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
            changed(machine);
            return new Result(Status.PROGRESSED, null);
        }
        changed(machine);
        if (progress.complete()) {
            BlueprintEvents.BUILT.invoker().built(new BlueprintBuiltEvent(machine, progress.blueprintId(), worker));
        }
        return new Result(Status.ADVANCED, null);
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
            changed(machine);
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
        changed(machine);
        return new Result(Status.ADVANCED, null);
    }

    /** Share of the built stages' materials left as debris when an unfinished result is destroyed. */
    public static final float BUILT_DEBRIS = 0.25F;
    /** Share of the materials already paid for the stage under way left as debris. */
    public static final float PAID_DEBRIS = 0.5F;

    /**
     * What is left of an unfinished result that is destroyed: a quarter of what its built stages took and half of what
     * was paid for the stage under way, each counted over all its stages. Tags come back as their {@code returns} item.
     */
    public static List<ItemStack> debris(UnderConstruction machine) {
        BuildProgress progress = machine.buildProgress();
        BlueprintConstructionPlan plan = progress.plan();
        Map<String, ItemStack> built = new java.util.LinkedHashMap<>();
        for (int index = 0; index < Math.min(progress.stage(), plan.stageCount()); index++) {
            for (Material material : plan.stage(index).materials()) {
                ItemStack stack = material.returnStack();
                built.merge(material.key(), stack, (have, more) -> have.copyWithCount(have.getCount() + more.getCount()));
            }
        }
        List<ItemStack> debris = new ArrayList<>();
        built.values().forEach(stack -> add(debris, stack, BUILT_DEBRIS));
        if (progress.currentStage() != null && progress.materialsCommitted()) {
            progress.currentStageRefund().forEach(stack -> add(debris, stack, PAID_DEBRIS));
        }
        return debris;
    }

    private static void add(List<ItemStack> debris, ItemStack stack, float share) {
        int count = (int) Math.floor(stack.getCount() * share);
        if (count > 0 && !stack.isEmpty()) {
            debris.add(stack.copyWithCount(count));
        }
    }

    private static void changed(UnderConstruction machine) {
        // Nobody rides an unfinished result, so taking a finished one apart sets its riders down.
        if (machine instanceof Entity entity && entity.isVehicle() && !machine.buildProgress().complete()) {
            entity.ejectPassengers();
        }
        machine.onBuildProgressChanged();
        ConstructionSync.changed(machine);
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
