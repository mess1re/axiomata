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

        if (machine instanceof UnderConstruction underConstruction) {
            underConstruction.buildProgress().begin(blueprintId, BlueprintItem.getQuality(blueprint));
            if (recipe != null && ConstructionStarters.isStarter(blueprint)) {
                // The starter is those stages, made already.
                underConstruction.buildProgress().skipBuilt(ConstructionStarters.builtStages(recipe, blueprint));
            }
            underConstruction.onDeployed(yaw);
            underConstruction.onBuildProgressChanged();
        } else {
            LOGGER.debug("{} does not support staged construction, placing it finished", blueprintId);
        }

        return level.addFreshEntity(machine) ? machine : null;
    }
}
