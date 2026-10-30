package me.mss1r.axiomata.blueprint.client;

import me.mss1r.axiomata.platform.AxiomataPlatform;

import me.mss1r.axiomata.blueprint.api.construction.ConstructionTarget;
import me.mss1r.axiomata.blueprint.internal.construction.ConstructionPlacementHelper;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
//? if <1.21 {
/*import me.mss1r.axiomata.platform.AxiomataPlatform;
*///?}

public final class PlacementTargetHelper {
    private PlacementTargetHelper() {
    }

    @Nullable
    public static ConstructionTarget getPlacementTarget(Minecraft minecraft, ItemStack resultStack,
                                                                       BlueprintDefinition recipe, float partialTick) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return null;
        }
        return ConstructionPlacementHelper.target(player, recipe, getPickRange(player), partialTick);
    }

    private static double getPickRange(LocalPlayer player) {
        //? if <1.21 {
        /*return AxiomataPlatform.blockReach(player);
        *///?} else {
        return player.blockInteractionRange();
        //?}
    }
}
