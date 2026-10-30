package me.mss1r.axiomata.blueprint.client.events;
import net.minecraftforge.client.event.RenderLevelStageEvent;
/** Native render-stage adapter retained for integrations. */
public class BlueprintClientGameEvents {
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            BlueprintPlacementPreview.render(event.getPoseStack(), event.getCamera().getPosition(), event.getPartialTick());
    }
}
