package me.mss1r.axiomata.blueprint.api.construction;

import com.mojang.logging.LogUtils;
import me.mss1r.axiomata.blueprint.api.ConstructionStarters;
import me.mss1r.axiomata.blueprint.internal.construction.ConstructionPlacementHelper;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.item.BlueprintItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import javax.annotation.Nullable;

public final class ConstructionDeployer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ConstructionDeployer() {
    }

    @Nullable
    public static Entity deploy(ServerLevel level, String blueprintId, ItemStack blueprint, ItemStack result,
                                BlockPos targetPos, Direction targetFace, Vec3 hitLocation, float yaw) {
        BlueprintDefinition recipe = BlueprintDefinitions.get(blueprintId);
        if (recipe == null) {
            return null;
        }
        ConstructionPlacementHelper.PlacementPlan plan = ConstructionPlacementHelper.findPlacement(
                level, result, targetPos, targetFace, hitLocation, yaw, recipe);
        if (plan == null || !plan.valid()) {
            return null;
        }

        Entity machine = ConstructionPlacementHelper.createPreviewEntity(
                level, result, plan.worldPos(), yaw, 0, recipe);
        if (machine == null) {
            LOGGER.warn("Blueprint {} has nothing to place in the world", blueprintId);
            return null;
        }

        if (!(machine instanceof UnderConstruction underConstruction)) {
            LOGGER.error("Blueprint {} builds {}, which does not support construction in the world", blueprintId,
                    recipe.result().entity());
            return null;
        }
        boolean started = ConstructionStarters.isStarter(blueprint);
        // Starter items were never drawn, so they build at the blueprint's exact cost.
        underConstruction.buildProgress().begin(blueprintId,
                started ? BuildQuality.EXACT : BlueprintItem.getQuality(blueprint));
        if (started) {
            underConstruction.buildProgress().skipBuilt(ConstructionStarters.builtStages(recipe, blueprint));
            if (underConstruction.buildProgress().complete()) {
                underConstruction.applyBuiltData(underConstruction.buildProgress().builtData());
            }
        }
        underConstruction.onDeployed(yaw);
        underConstruction.onBuildProgressChanged();

        return level.addFreshEntity(machine) ? machine : null;
    }
}
