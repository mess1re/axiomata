package me.mss1r.axiomata.loading;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

final class LoadingTools {
    private LoadingTools() {}

    static void damageHeldItem(ItemStack stack, int amount, LivingEntity owner, InteractionHand hand) {
        //? if forge {
        /*stack.hurtAndBreak(amount, owner, entity -> entity.broadcastBreakEvent(hand));
        *///?} else {
        stack.hurtAndBreak(amount, owner, LivingEntity.getSlotForHand(hand));
        //?}
    }
}
