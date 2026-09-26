package me.mss1r.axiomata.blueprint.api.construction;

import net.minecraft.world.entity.player.Player;

/**
 * Implemented by the real entity being assembled. There is no temporary construction-site entity,
 * so consumers must gate riding, firing and animation on {@link #isFullyBuilt()} themselves.
 */
public interface UnderConstruction {
    BuildProgress buildProgress();
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
        return buildProgress().complete();
    }
}
