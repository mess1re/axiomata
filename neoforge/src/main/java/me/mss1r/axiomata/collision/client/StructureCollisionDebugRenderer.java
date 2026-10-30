package me.mss1r.axiomata.collision.client;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
/** Native render-stage adapter retained for integrations. */
public final class StructureCollisionDebugRenderer {
    private StructureCollisionDebugRenderer() {}
    public static void setEnabled(boolean enabled) { StructureCollisionDebugDraw.setEnabled(enabled); }
    public static boolean isEnabled() { return StructureCollisionDebugDraw.isEnabled(); }
    public static void renderCollision(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES)
            StructureCollisionDebugDraw.renderCollision(event.getPoseStack(), event.getCamera().getPosition());
    }
}
