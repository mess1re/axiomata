package me.mss1r.axiomata.blueprint.api.construction;

import com.mojang.logging.LogUtils;
import me.mss1r.axiomata.blueprint.internal.construction.ConstructionPlacementHelper;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.item.BlueprintItem;
import me.mss1r.axiomata.blueprint.api.event.BlueprintEvents;
import me.mss1r.axiomata.blueprint.api.event.BlueprintPlacementCheckEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
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
        return deploy(level, blueprintId, blueprint, result, ConstructionTarget.of(
                new BlockHitResult(hitLocation, targetFace, targetPos, false)), yaw);
    }

    /** Deploys on the aimed block or on the deck of the aimed structure, which then carries the build. */
    @Nullable
    public static Entity deploy(ServerLevel level, String blueprintId, ItemStack blueprint, ItemStack result,
                                ConstructionTarget target, float yaw) {
        return deploy(level, blueprintId, blueprint, result, target, yaw, null);
    }

    @Nullable
    public static Entity deploy(ServerLevel level, String blueprintId, ItemStack blueprint, ItemStack result,
                                ConstructionTarget target, float yaw, @Nullable LivingEntity worker) {
        BlueprintDefinition recipe = BlueprintDefinitions.get(blueprintId);
        if (recipe == null) {
            return null;
        }
        ConstructionPlacementHelper.PlacementPlan plan = ConstructionPlacementHelper.findPlacement(
                level, result, target, yaw, recipe);
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
        underConstruction.buildProgress().begin(blueprintId, BlueprintItem.getQuality(blueprint));
        underConstruction.onDeployed(yaw);
        underConstruction.onBuildProgressChanged();

        BlueprintPlacementCheckEvent check = new BlueprintPlacementCheckEvent(underConstruction, blueprintId, worker);
        BlueprintEvents.PLACEMENT_CHECK.invoker().check(check);
        if (!check.allowed()) return null;

        return level.addFreshEntity(machine) ? machine : null;
    }
}
