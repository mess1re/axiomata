package me.mss1r.axiomata.blueprint.api.construction;

import net.minecraft.world.entity.player.Player;

import java.util.Map;

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

    /**
     * Told what a build that ended before its last stage came to: the result's data added up over the stages that
     * were built. Override to shape the finished entity to it, for instance a ladder to the sections it has.
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
