package me.mss1r.axiomata.blueprint.client.events;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
/** Native render-stage adapter retained for integrations. */
public class BlueprintClientGameEvents {
    public static void onRenderGui(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
        me.mss1r.axiomata.blueprint.client.renderer.ConstructionStageSigns.render(event.getGuiGraphics());
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY)
            me.mss1r.axiomata.blueprint.client.renderer.ConstructionStageSigns.beginFrame();
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            BlueprintPlacementPreview.render(event.getPoseStack(), event.getCamera().getPosition(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }
}
