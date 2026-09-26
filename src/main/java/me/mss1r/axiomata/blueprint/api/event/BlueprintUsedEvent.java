package me.mss1r.axiomata.blueprint.api.event;

import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class BlueprintUsedEvent {
    private final ServerPlayer player;
    private final String recipeId;
    private final BlueprintDefinition recipe;
    private final ItemStack result;
    private final boolean buildsInWorld;

    public BlueprintUsedEvent(ServerPlayer player, String recipeId, BlueprintDefinition recipe,
                              ItemStack result, boolean buildsInWorld) {
        this.player = player;
        this.recipeId = recipeId;
        this.recipe = recipe;
        this.result = result.copy();
        this.buildsInWorld = buildsInWorld;
    }

    public ServerPlayer player() {
        return player;
    }

    public String recipeId() {
        return recipeId;
    }

    public BlueprintDefinition recipe() {
        return recipe;
    }

    public ItemStack result() {
        return result.copy();
    }

    public boolean buildsInWorld() {
        return buildsInWorld;
    }
}
