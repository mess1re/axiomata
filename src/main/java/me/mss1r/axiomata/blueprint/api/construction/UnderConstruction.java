package me.mss1r.axiomata.blueprint.api.construction;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import me.mss1r.axiomata.blueprint.api.visual.BlueprintConstructionVisuals;
import me.mss1r.axiomata.blueprint.internal.construction.ConstructionSync;


/**
 * Implemented by the real entity being assembled. There is no temporary construction-site entity. While it is
 * unfinished Axiomata keeps it still, lets no one ride it, saves and synchronizes its {@link BuildProgress} and leaves
 * debris when it is killed; the result gates only its own actions, such as firing, on {@link #isFullyBuilt()}.
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

    /**
     * Whether a block may carry this result while it is built on the ground, e.g. a slipway that refuses soil. Only
     * sturdy blocks are asked; enough of the footprint must still rest on accepted ones.
     */
    default boolean acceptsSupport(BlockPos pos, BlockState state) {
        return true;
    }

    /** Who may build or take apart this result, e.g. only its owner's side. Others cannot touch it with a hammer. */
    default boolean mayWork(Player builder) {
        return true;
    }

    /** A blow counts when the builder's aim lands on the section being assembled, by the construction markup. */
    default boolean acceptsBlowOnStage(Player builder, String section) {
        ResourceLocation model = constructionModel();
        return !(this instanceof Entity result) || model == null
                || ConstructionHitTesting.accepts(builder, result, model, section);
    }

    default boolean isFullyBuilt() {
        return this instanceof Entity entity && entity.level().isClientSide()
                ? constructionStatus().complete()
                : buildProgress().complete();
    }
}
