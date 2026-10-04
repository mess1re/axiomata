package me.mss1r.axiomata.blueprint.menu;

import me.mss1r.axiomata.blueprint.api.BlueprintTags;
import me.mss1r.axiomata.blueprint.api.ConstructionStarters;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.registry.BlueprintMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class BlueprintUseMenu extends AbstractContainerMenu {
    private final BlueprintDefinition recipe;
    private final ItemStack blueprint;
    private final String recipeId;

    public BlueprintUseMenu(int id, Inventory playerInv, BlueprintDefinition recipe, ItemStack blueprint, String recipeId) {
        super(BlueprintMenus.BLUEPRINT_USE_MENU.get(), id);
        this.recipe = recipe;
        this.blueprint = blueprint;
        this.recipeId = recipeId;
    }

    public BlueprintDefinition getRecipe() { return recipe; }
    public String getRecipeId() { return recipeId; }

    @Override
    public boolean stillValid(Player player) {
        return recipeId.equals(ConstructionStarters.definitionOf(player.getOffhandItem()))
                && player.getMainHandItem().is(BlueprintTags.CONSTRUCTION_HAMMERS);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
