package me.mss1r.axiomata.blueprint.menu;

import me.mss1r.axiomata.blueprint.item.BlueprintItem;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.registry.BlueprintMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import java.util.ArrayList;
import java.util.List;

public class BlueprintUseMenu extends AbstractContainerMenu {
    private final BlueprintDefinition recipe;
    private final String recipeId;
    private final InteractionHand hand;

    public BlueprintUseMenu(int id, Inventory playerInv, BlueprintDefinition recipe, ItemStack blueprint, String recipeId) {
        this(id, playerInv, recipe, blueprint, recipeId,
                blueprint == playerInv.player.getMainHandItem() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }

    public BlueprintUseMenu(int id, Inventory playerInv, BlueprintDefinition recipe, ItemStack blueprint, String recipeId,
                            InteractionHand hand) {
        super(BlueprintMenus.BLUEPRINT_USE_MENU.get(), id);
        this.recipe = recipe;
        this.recipeId = recipeId;
        this.hand = hand;
    }

    public BlueprintDefinition getRecipe() { return recipe; }
    public String getRecipeId() { return recipeId; }
    public InteractionHand getHand() { return hand; }

    /** Reserve the blueprint being used, even if a recipe accepts other blueprints as a material. */
    public List<ItemStack> materialStacks(Player player) {
        List<ItemStack> stacks = new ArrayList<>(player.getInventory().items);
        stacks.addAll(player.getInventory().offhand);
        ItemStack blueprint = player.getItemInHand(hand);
        stacks.removeIf(stack -> stack == blueprint);
        return stacks;
    }

    @Override
    public boolean stillValid(Player player) {
        return recipe != null && BlueprintItem.canUse(player, recipe, recipeId, hand);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
