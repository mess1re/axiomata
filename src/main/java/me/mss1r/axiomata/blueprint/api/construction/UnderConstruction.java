package me.mss1r.axiomata.blueprint.api.construction;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import me.mss1r.axiomata.blueprint.api.visual.BlueprintConstructionVisuals;
import me.mss1r.axiomata.blueprint.internal.construction.ConstructionSync;


/**
 * Implemented by the real entity being assembled. There is no temporary construction-site entity,
 * so consumers must gate riding, firing and animation on {@link #isFullyBuilt()} themselves.
 * Axiomata keeps clients informed of the progress; the entity only saves its {@link BuildProgress}.
 */
public interface UnderConstruction {
    BuildProgress buildProgress();

    /** Override if the construction markup has a different ID from the entity type. */
    default ResourceLocation constructionModel() {
        return this instanceof Entity entity ? BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()) : null;
    }

    /** The progress on the server, or on a client the progress Axiomata last sent it. */
    default ConstructionStatus constructionStatus() {
        return ConstructionSync.status(this);
    }

    default BlueprintConstructionVisuals.State constructionVisualState() {
        ConstructionStatus status = constructionStatus();
        return BlueprintConstructionVisuals.state(status.blueprintId(),
                status.complete() ? Integer.MAX_VALUE : status.builtStages());
    }
    /** Apply every heading field used by both the preview and the deployed entity. */
    default void orientForPlacement(float yaw) {
    }

    default void onDeployed(float yaw) {
        orientForPlacement(yaw);
    }

    default void onBuildProgressChanged() {
    }

    /** Override when hammer hits should land on the section currently being assembled. */
    default boolean acceptsBlowOnStage(Player builder, String section) {
        return true;
    }

    default boolean isFullyBuilt() {
        return this instanceof Entity entity && entity.level().isClientSide()
                ? constructionStatus().complete()
                : buildProgress().complete();
    }
}
