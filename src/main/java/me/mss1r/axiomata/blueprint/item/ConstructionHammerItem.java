package me.mss1r.axiomata.blueprint.item;

import me.mss1r.axiomata.blueprint.api.ConstructionStarters;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.registry.BlueprintItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class ConstructionHammerItem extends Item {
    public ConstructionHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand == InteractionHand.MAIN_HAND && tryUseOffhandBlueprint(level, player)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && context.getHand() == InteractionHand.MAIN_HAND
                && tryUseOffhandBlueprint(context.getLevel(), player)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static boolean tryUseOffhandBlueprint(Level level, Player player) {
        ItemStack offhand = player.getOffhandItem();
        if (offhand.isEmpty()) {
            return false;
        }
        if (offhand.getItem() == BlueprintItems.BLUEPRINT.get()) {
            offhand.getItem().use(level, player, InteractionHand.OFF_HAND);
            return true;
        }
        String startedBlueprint = ConstructionStarters.definitionFor(offhand);
        if (startedBlueprint == null) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            BlueprintDefinition definition = BlueprintDefinitions.get(startedBlueprint);
            if (definition != null && ConstructionStarters.builtStages(definition, offhand) >= definition.stageCount()) {
                serverPlayer.displayClientMessage(
                        Component.translatable("message.axiomata.construction.starter_complete"), true);
            } else {
                BlueprintItem.openUseMenu(serverPlayer, startedBlueprint, offhand);
            }
        }
        return true;
    }
}
