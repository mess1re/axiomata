package me.mss1r.axiomata.ballistics;

import me.mss1r.axiomata.platform.AxiomataPlatform;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
public final class ProjectileCombat {
    private ProjectileCombat() {}

    @Nullable
    public static Entity logicalTarget(@Nullable Entity target) {
        return AxiomataPlatform.logicalTarget(target);
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
        if (!AxiomataPlatform.blocksWithShield(activeItem)) {
            return;
        }

        //? if >=1.21 {
        target.onEquippedItemBroken(activeItem.getItem(), LivingEntity.getSlotForHand(target.getUsedItemHand()));
        //?}
        activeItem.shrink(1);
        //? if <1.21 {
        /*target.broadcastBreakEvent(target.getUsedItemHand());
        *///?}
        target.stopUsingItem();
    }
}
