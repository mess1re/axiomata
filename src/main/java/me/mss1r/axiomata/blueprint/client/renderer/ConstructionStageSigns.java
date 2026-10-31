package me.mss1r.axiomata.blueprint.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.api.visual.BlueprintConstructionVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.LinkedHashMap;
import java.util.Map;

/** World-anchored progress labels rendered by the GUI, independently of entity shaders and buffers. */
public final class ConstructionStageSigns {
    private record Sign(Entity entity, Matrix4f pose, Component text) {}
    private static final Map<Entity, Sign> PENDING = new LinkedHashMap<>();

    private ConstructionStageSigns() {}

    public static void beginFrame() {
        PENDING.clear();
    }

    public static <T extends Entity & UnderConstruction> void submit(T entity, double height, PoseStack poses) {
        var minecraft = Minecraft.getInstance();
        var status = entity.constructionStatus();
        var recipe = BlueprintDefinitions.get(status.blueprintId());
        if (status.complete() || recipe == null || recipe.stages().isEmpty()) return;
        int stage = Math.min(status.builtStages(), recipe.stageCount() - 1);
        Component text = Component.empty()
                .append(BlueprintConstructionVisuals.stageName(status.blueprintId(), recipe.stages().get(stage).section()))
                .append(Component.literal(String.format("  %d/%d  ·  %d/%d", status.hits(),
                        Math.max(1, status.stageHits()), stage + 1, recipe.stageCount())));
        Matrix4f clip = new Matrix4f(RenderSystem.getProjectionMatrix())
                .mul(RenderSystem.getModelViewMatrix()).mul(poses.last().pose())
                .translate(0, (float)height, 0)
                .rotate(minecraft.getEntityRenderDispatcher().cameraOrientation());
        //? if <1.21 {
        /*clip.scale(-0.025F, -0.025F, 0.025F);
        *///?} else {
        clip.scale(0.025F, -0.025F, 0.025F);
        //?}
        Vector4f center = clip.transform(new Vector4f(0, 0, 0, 1));
        Vector4f right = clip.transform(new Vector4f(1, 0, 0, 1));
        Vector4f down = clip.transform(new Vector4f(0, 1, 0, 1));
        if (center.w <= 0.05F || right.w <= 0 || down.w <= 0) return;
        float halfWidth = minecraft.getWindow().getGuiScaledWidth() * 0.5F;
        float halfHeight = minecraft.getWindow().getGuiScaledHeight() * 0.5F;
        float x = center.x / center.w;
        float y = center.y / center.w;
        Matrix4f screen = new Matrix4f()
                .m00((right.x / right.w - x) * halfWidth)
                .m01(-(right.y / right.w - y) * halfHeight)
                .m10((down.x / down.w - x) * halfWidth)
                .m11(-(down.y / down.w - y) * halfHeight)
                .m30((x + 1) * halfWidth).m31((1 - y) * halfHeight).m32(400);
        PENDING.put(entity, new Sign(entity, screen, text));
    }

    public static void render(GuiGraphics graphics) {
        var minecraft = Minecraft.getInstance();
        try {
            if (PENDING.isEmpty() || minecraft.level == null || minecraft.player == null || minecraft.options.hideGui) return;
            graphics.flush();
            // Finish every backing rectangle before submitting any foreground glyphs.
            for (int pass = 0; pass < 2; pass++) {
                for (Sign sign : PENDING.values()) {
                    if (sign.entity.isRemoved() || sign.entity.level() != minecraft.level) continue;
                    graphics.pose().pushPose();
                    try {
                        graphics.pose().last().pose().mul(sign.pose);
                        int width = minecraft.font.width(sign.text);
                        int x = -width / 2;
                        if (pass == 0) graphics.fill(x - 1, -1, x + width + 1, minecraft.font.lineHeight, 0x40000000);
                        else {
                            graphics.pose().translate(0, 0, 1);
                            graphics.drawString(minecraft.font, sign.text, x, 0, 0xFFE8DFC8, false);
                        }
                    } finally {
                        graphics.pose().popPose();
                    }
                }
                graphics.flush();
            }
        } finally {
            PENDING.clear();
        }
    }
}
