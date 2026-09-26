package me.mss1r.axiomata.blueprint.api.event;

import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import net.minecraft.server.level.ServerPlayer;

public final class BlueprintRecipeCheckEvent {
    public final ServerPlayer player;
    public final BlueprintDefinition recipe;
    public final String recipeId;
    public boolean allowed = true;

    public BlueprintRecipeCheckEvent(ServerPlayer player, BlueprintDefinition recipe, String recipeId) {
        this.player = player;
        this.recipe = recipe;
        this.recipeId = recipeId;
    }

    public void deny() { allowed = false; }
}
