package me.mss1r.axiomata.blueprint.api;

import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.event.BlueprintEvents;
import me.mss1r.axiomata.blueprint.api.event.BlueprintRecipeCheckEvent;
import net.minecraft.server.level.ServerPlayer;

public final class BlueprintPermissions {
    private BlueprintPermissions() {
    }

    public static boolean canUse(ServerPlayer player, BlueprintDefinition recipe, String recipeId) {
        BlueprintRecipeCheckEvent event = new BlueprintRecipeCheckEvent(player, recipe, recipeId);
        BlueprintEvents.RECIPE_CHECK.invoker().check(event);
        return event.allowed;
    }
}
