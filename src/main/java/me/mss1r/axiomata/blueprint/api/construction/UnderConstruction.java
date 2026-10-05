package me.mss1r.axiomata.blueprint.api.construction;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import me.mss1r.axiomata.blueprint.api.visual.BlueprintConstructionVisuals;

import java.util.Map;

/**
 * Implemented by the real entity being assembled. There is no temporary construction-site entity,
 * so consumers must gate riding, firing and animation on {@link #isFullyBuilt()} themselves.
 */
public interface UnderConstruction {
    BuildProgress buildProgress();

    /** Override if the section resource has a different ID from the entity type. */
    default ResourceLocation constructionModel() {
        return this instanceof Entity entity ? BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()) : null;
    }

    /** Use synchronised progress here when the client does not keep a full build plan. */
    default BlueprintConstructionVisuals.State constructionVisualState() {
        return BlueprintConstructionVisuals.state(buildProgress().blueprintId(), buildProgress().builtStages());
    }
    /** Apply every heading field used by both the preview and the deployed entity. */
    default void orientForPlacement(float yaw) {
    }

    default void onDeployed(float yaw) {
        orientForPlacement(yaw);
    }

    default void onBuildProgressChanged() {
    }

    /**
     * Called when a build is ended early, with the summed {@code adds} of the built stages. Override to adjust the
     * entity, e.g. a ladder's section count.
     */
    default void applyBuiltData(Map<String, Integer> data) {
    }
    /** Override when hammer hits should land on the section currently being assembled. */
    default boolean acceptsBlowOnStage(Player builder, String section) {
        return true;
    }

    default boolean isFullyBuilt() {
        return buildProgress().complete();
    }
}
