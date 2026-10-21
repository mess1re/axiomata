package me.mss1r.axiomata.blueprint.client;

import me.mss1r.axiomata.blueprint.api.construction.ConstructionTarget;
import me.mss1r.axiomata.blueprint.internal.construction.ConstructionPlacementHelper;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
//? if forge {
/*import net.minecraftforge.common.ForgeMod;
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
        //? if forge {
        /*return player.getAttributeValue(ForgeMod.BLOCK_REACH.get());
        *///?} else {
        return player.blockInteractionRange();
        //?}
    }
}
