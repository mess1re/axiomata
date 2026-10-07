package me.mss1r.axiomata.ballistics;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
//? if forge {
/*import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.common.ToolActions;
*///?} else {
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.common.ItemAbilities;
//?}

public final class ProjectileCombat {
    private ProjectileCombat() {}

    @Nullable
    public static Entity logicalTarget(@Nullable Entity target) {
        if (target instanceof PartEntity<?> part) {
            return part.getParent();
        }
        return target;
    }

    @Nullable
    public static LivingEntity livingTarget(Entity target) {
        Entity logicalTarget = logicalTarget(target);
        return logicalTarget instanceof LivingEntity living ? living : null;
    }

    public static int targetId(Entity target) {
        return logicalTarget(target).getId();
    }

    public static void breakBlockingShield(LivingEntity target) {
        if (!target.isBlocking()) {
            return;
        }

        ItemStack activeItem = target.getUseItem();
        //? if forge {
        /*if (!activeItem.canPerformAction(ToolActions.SHIELD_BLOCK)) {
        *///?} else {
        if (!activeItem.canPerformAction(ItemAbilities.SHIELD_BLOCK)) {
        //?}
            return;
        }

        //? if neoforge {
        target.onEquippedItemBroken(activeItem.getItem(), LivingEntity.getSlotForHand(target.getUsedItemHand()));
        //?}
        activeItem.shrink(1);
        //? if forge {
        /*target.broadcastBreakEvent(target.getUsedItemHand());
        *///?}
        target.stopUsingItem();
    }
}
