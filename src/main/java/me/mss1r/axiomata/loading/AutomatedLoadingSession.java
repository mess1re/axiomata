package me.mss1r.axiomata.loading;


import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class AutomatedLoadingSession {
    private UUID operatorUuid;
    private int stage = -1;
    private int remainingTicks;
    private int totalTicks;

    public boolean needsStart(LivingEntity operator, int stage) {
        return !operator.getUUID().equals(operatorUuid) || this.stage != stage;
    }

    public void start(LivingEntity operator, int stage, int durationTicks) {
        operatorUuid = operator.getUUID();
        this.stage = stage;
        totalTicks = Math.max(1, durationTicks);
        remainingTicks = totalTicks;
    }

    public boolean tickComplete() {
        return --remainingTicks <= 0;
    }

    public boolean isActive() {
        return remainingTicks > 0;
    }

    public boolean isStage(int stage) {
        return isActive() && this.stage == stage;
    }

    public int elapsedTicks() {
        return Math.max(0, totalTicks - remainingTicks);
    }

    public void cancel(LivingEntity operator) {
        if (operator.getUUID().equals(operatorUuid)) {
            reset();
        }
    }

    public void reset() {
        operatorUuid = null;
        stage = -1;
        remainingTicks = 0;
        totalTicks = 0;
    }

    public static Item firstAvailable(Container inventory, Item... items) {
        for (Item item : items) {
            if (count(inventory, item) > 0) {
                return item;
            }
        }
        return null;
    }

    public static boolean hasRequiredItem(Container inventory, LoadingRequirement stage, Item stageItem) {
        return count(inventory, stageItem) >= stage.requiredStackCount();
    }

    public static boolean finishStage(Container inventory, LoadingRequirement stage, Item stageItem,
                                      LivingEntity operator) {
        return switch (stage.itemPolicy()) {
            case CONSUME -> consume(inventory, stageItem, stage.amount());
            case DAMAGE_TOOL -> damageTool(inventory, stageItem, stage.durabilityCost(), operator);
        };
    }

    private static boolean damageTool(Container inventory, Item item, int durabilityCost,
                                      LivingEntity operator) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(item)) {
                continue;
            }
            LoadingTools.damageHeldItem(stack, durabilityCost, operator, InteractionHand.MAIN_HAND);
            inventory.setChanged();
            return true;
        }
        return false;
    }

    private static int count(Container inventory, Item item) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static boolean consume(Container inventory, Item item, int amount) {
        if (count(inventory, item) < amount) {
            return false;
        }

        int remaining = amount;
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(item)) {
                continue;
            }
            int consumed = Math.min(remaining, stack.getCount());
            stack.shrink(consumed);
            remaining -= consumed;
        }
        inventory.setChanged();
        return true;
    }
}
