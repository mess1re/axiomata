package me.mss1r.axiomata.collision.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.mss1r.axiomata.Axiomata;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPart;
import me.mss1r.axiomata.collision.StructureTransform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
*///?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
//?}
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

//? if forge {
/*@Mod.EventBusSubscriber(modid = Axiomata.MOD_ID, value = Dist.CLIENT)
*///?} else {
@EventBusSubscriber(modid = Axiomata.MOD_ID, value = Dist.CLIENT)
//?}
public final class StructureCollisionDebugRenderer {
    private static final double MAX_DISTANCE_SQR = 128.0D * 128.0D;
    private static final int[][] EDGES = {
            {0, 1}, {0, 2}, {0, 4},
            {1, 3}, {1, 5},
            {2, 3}, {2, 6},
            {3, 7},
            {4, 5}, {4, 6},
            {5, 7},
            {6, 7}
    };
    private static final DebugColor[] GROUP_COLORS = {
            new DebugColor(0.2F, 1.0F, 0.25F),
            new DebugColor(1.0F, 0.55F, 0.1F),
            new DebugColor(0.1F, 0.85F, 1.0F),
            new DebugColor(1.0F, 0.2F, 0.85F),
            new DebugColor(1.0F, 0.95F, 0.15F),
            new DebugColor(0.55F, 0.35F, 1.0F)
    };
    private static boolean enabled;

    private StructureCollisionDebugRenderer() {
    }

    public static void setEnabled(boolean enabled) {
        StructureCollisionDebugRenderer.enabled = enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    @SubscribeEvent
    public static void renderCollision(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!enabled || minecraft.level == null) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof CollidableStructure structure
                    && entity.distanceToSqr(camera) <= MAX_DISTANCE_SQR) {
                renderStructure(poseStack, lines, structure);
            }
        }
        poseStack.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static void renderStructure(PoseStack poseStack, VertexConsumer lines,
                                        CollidableStructure structure) {
        StructureTransform transform = structure.collisionTransform();
        for (CollisionGroup group : structure.collisionGroups()) {
            DebugColor color = colorFor(group.name());
            for (CollisionPart part : group.parts()) {
                renderPart(poseStack, lines, transform, group, part, color);
            }
        }
    }

    private static void renderPart(PoseStack poseStack, VertexConsumer lines, StructureTransform transform,
                                   CollisionGroup group, CollisionPart part, DebugColor color) {
        AABB box = part.box();
        Vec3[] corners = new Vec3[8];
        for (int index = 0; index < corners.length; index++) {
            Vec3 partPoint = new Vec3(
                    (index & 4) == 0 ? box.minX : box.maxX,
                    (index & 2) == 0 ? box.minY : box.maxY,
                    (index & 1) == 0 ? box.minZ : box.maxZ);
            corners[index] = transform.toWorld(group.fromPart(part, partPoint));
        }

        PoseStack.Pose pose = poseStack.last();
        Matrix4f position = pose.pose();
        Matrix3f normal = pose.normal();
        for (int[] edge : EDGES) {
            line(lines, position, normal, corners[edge[0]], corners[edge[1]], color);
        }
    }

    private static void line(VertexConsumer consumer, Matrix4f position, Matrix3f normal,
                             Vec3 from, Vec3 to, DebugColor color) {
        Vec3 direction = to.subtract(from).normalize();
        vertex(consumer, position, normal, from, direction, color);
        vertex(consumer, position, normal, to, direction, color);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f position, Matrix3f normal,
                               Vec3 point, Vec3 direction, DebugColor color) {
        //? if forge {
        /*consumer.vertex(position, (float) point.x, (float) point.y, (float) point.z)
                .color(color.red(), color.green(), color.blue(), 1.0F)
                .normal(normal, (float) direction.x, (float) direction.y, (float) direction.z)
                .endVertex();
        *///?} else {
        Vector3f transformedNormal = normal.transform(new Vector3f(
                (float) direction.x, (float) direction.y, (float) direction.z));
        consumer.addVertex(position, (float) point.x, (float) point.y, (float) point.z)
                .setColor(color.red(), color.green(), color.blue(), 1.0F)
                .setNormal(transformedNormal.x(), transformedNormal.y(), transformedNormal.z());
        //?}
    }

    private static DebugColor colorFor(String groupName) {
        return GROUP_COLORS[Math.floorMod(groupName.hashCode(), GROUP_COLORS.length)];
    }

    private record DebugColor(float red, float green, float blue) {
    }
}
