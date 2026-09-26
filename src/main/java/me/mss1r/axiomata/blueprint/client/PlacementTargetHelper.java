package me.mss1r.axiomata.blueprint.client;

import me.mss1r.axiomata.blueprint.internal.construction.ConstructionPlacementHelper;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
//? if forge {
/*import net.minecraftforge.common.ForgeMod;
*///?}

public final class PlacementTargetHelper {
    private PlacementTargetHelper() {
    }

    @Nullable
    public static BlockHitResult getPlacementTarget(Minecraft minecraft, ItemStack resultStack,
                                                     BlueprintDefinition recipe, float partialTick) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return null;
        }

        HitResult hitResult = player.pick(getPickRange(player), partialTick,
                ConstructionPlacementHelper.requiresFluidTargeting(minecraft.level, resultStack, recipe));
        return hitResult instanceof BlockHitResult blockHitResult && hitResult.getType() == HitResult.Type.BLOCK
                ? blockHitResult
                : null;
    }

    private static double getPickRange(LocalPlayer player) {
        //? if forge {
        /*return player.getAttributeValue(ForgeMod.BLOCK_REACH.get());
        *///?} else {
        return player.blockInteractionRange();
        //?}
    }
}
