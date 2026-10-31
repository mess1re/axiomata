package me.mss1r.axiomata.blueprint.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
//? if <1.21 {
/*import com.mojang.blaze3d.vertex.BufferBuilder;
*///?} else {
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
//?}
import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.api.visual.BlueprintConstructionVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

import java.util.LinkedHashMap;
import java.util.Map;

/** Depth-tested world labels drawn after entity geometry with their own buffers. */
public final class ConstructionStageSigns {
    private record Sign(Entity entity, Matrix4f pose, Component text) {}
    private static final Map<Entity, Sign> PENDING = new LinkedHashMap<>();
    //? if <1.21 {
    /*private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(new BufferBuilder(4096));
    *///?} else {
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(new ByteBufferBuilder(4096));
    //?}

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
        // Save the view transform as well: the late world callback may run after it is reset.
        Matrix4f pose = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(poses.last().pose())
                .translate(0, (float)height, 0)
                .rotate(minecraft.getEntityRenderDispatcher().cameraOrientation());
        //? if <1.21 {
        /*pose.scale(-0.025F, -0.025F, 0.025F);
        *///?} else {
        pose.scale(0.025F, -0.025F, 0.025F);
        //?}
        PENDING.put(entity, new Sign(entity, pose, text));
    }

    public static void render() {
        var minecraft = Minecraft.getInstance();
        try {
            if (PENDING.isEmpty() || minecraft.level == null || minecraft.player == null || minecraft.options.hideGui) return;
            Matrix4f inverseView = new Matrix4f(RenderSystem.getModelViewMatrix()).invert();
            // Flush the backing rectangles first; keep them behind the glyph plane.
            // Both passes test against the world's depth, including opaque entity geometry.
            for (int pass = 0; pass < 2; pass++) {
                for (Sign sign : PENDING.values()) {
                    if (sign.entity.isRemoved() || sign.entity.level() != minecraft.level) continue;
                    Matrix4f pose = new Matrix4f(inverseView);
                    // Camera-space Z avoids the opposite billboard Z axes used by different versions.
                    if (pass == 0) pose.translate(0, 0, -0.01F);
                    pose.mul(sign.pose);
                    float x = -minecraft.font.width(sign.text) / 2.0F;
                    if (pass == 0) {
                        VertexConsumer vertices = BUFFERS.getBuffer(RenderType.textBackground());
                        float right = x + minecraft.font.width(sign.text) + 1;
                        vertex(vertices, pose, x - 1, -1);
                        vertex(vertices, pose, x - 1, minecraft.font.lineHeight);
                        vertex(vertices, pose, right, minecraft.font.lineHeight);
                        vertex(vertices, pose, right, -1);
                    } else {
                        minecraft.font.drawInBatch(sign.text, x, 0, 0xFFE8DFC8, false, pose, BUFFERS,
                                Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
                    }
                }
                BUFFERS.endBatch();
            }
        } finally {
            PENDING.clear();
        }
    }

    private static void vertex(VertexConsumer vertices, Matrix4f pose, float x, float y) {
        //? if <1.21 {
        /*vertices.vertex(pose, x, y, 0).color(0, 0, 0, 64).uv2(LightTexture.FULL_BRIGHT).endVertex();
        *///?} else {
        vertices.addVertex(pose, x, y, 0).setColor(0, 0, 0, 64).setLight(LightTexture.FULL_BRIGHT);
        //?}
    }
}
