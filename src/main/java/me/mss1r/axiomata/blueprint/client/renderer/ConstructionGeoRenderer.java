package me.mss1r.axiomata.blueprint.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.blueprint.api.BlueprintTags;
import me.mss1r.axiomata.blueprint.api.construction.ConstructionStatus;
import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.api.visual.BlueprintConstructionVisuals;
import me.mss1r.axiomata.structure.StructureSections;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
//? if forge {
/*import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.object.Color;
*///?} else {
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.util.Color;
//?}
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws an unfinished build from its construction markup: built cubes as they are, the section being worked on
 * pulsing and the sections still to come as a pale plan. Only a builder sees the plan and the stage sign; everyone
 * else sees what stands. Requires GeckoLib; extend it in place of {@link GeoEntityRenderer}.
 */
public abstract class ConstructionGeoRenderer<T extends Entity & GeoAnimatable & UnderConstruction>
        extends GeoEntityRenderer<T> {
    /** How far from a build a player with a hammer still sees its plan. */
    protected static final double PLAN_RANGE = 32.0D;

    private static final float GHOST_ALPHA = 0.20F;
    private static final Color GHOST_TINT = Color.ofRGBA(0.62F, 0.74F, 1.0F, 1.0F);
    private static final float ACTIVE_BREATH_TICKS = 40.0F;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<ResourceLocation> WARNED = ConcurrentHashMap.newKeySet();

    private StructureSections sections;
    private Set<String> builtSections = Set.of();
    private MultiBufferSource bufferSource;
    private RenderType builtType;
    private RenderType ghostType;
    private RenderType activeType;
    private String activeSection = "";
    private float activePulseHigh;
    private float activePulseLow;
    private boolean building;

    protected ConstructionGeoRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
    }

    /** Whether the local player sees the plan: a construction hammer in hand, close enough to work. */
    protected boolean showsPlan(T entity) {
        Player player = Minecraft.getInstance().player;
        return player != null && player.getMainHandItem().is(BlueprintTags.CONSTRUCTION_HAMMERS)
                && player.distanceToSqr(entity) <= PLAN_RANGE * PLAN_RANGE;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        building = !entity.isFullyBuilt();
        sections = building ? sectionsFor(entity) : null;
        building = building && sections != null;
        boolean plan = building && showsPlan(entity);
        bufferSource = buffer;
        builtType = building ? getRenderType(entity, getTextureLocation(entity), buffer, partialTick) : null;
        ghostType = plan ? RenderType.entityTranslucent(getTextureLocation(entity)) : null;
        activeType = plan ? ConstructionHighlightRenderType.of(getTextureLocation(entity)) : null;
        BlueprintConstructionVisuals.State state = building
                ? entity.constructionVisualState()
                : new BlueprintConstructionVisuals.State(Set.of(), "", 0, 0);
        builtSections = state.builtSections();
        activeSection = state.activeSection();
        double phase = (entity.tickCount + partialTick) * Math.PI * 2.0D / ACTIVE_BREATH_TICKS;
        float breath = (float) (0.5D - 0.5D * Math.cos(phase));
        int packedPulse = Math.round(breath * 65535.0F);
        activePulseHigh = ((packedPulse >>> 8) & 0xFF) / 255.0F;
        activePulseLow = (packedPulse & 0xFF) / 255.0F;

        try {
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            if (plan) {
                renderStageSign(entity, poseStack, buffer, packedLight);
            }
        } finally {
            building = false;
            builtType = null;
            ghostType = null;
            activeType = null;
            bufferSource = null;
        }
    }

    @Override
    //? if forge {
    /*public void renderRecursively(PoseStack poseStack, T entity, GeoBone bone, RenderType renderType,
                                  MultiBufferSource buffers, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
    *///?} else {
    public void renderRecursively(PoseStack poseStack, T entity, GeoBone bone, RenderType renderType,
                                  MultiBufferSource buffers, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay, int packedColor) {
    //?}
        var previous = building ? bone.saveSnapshot() : null;
        if (building) {
            // The markup is exported in the authored pose, not in whatever an animation is doing.
            var initial = bone.getInitialSnapshot();
            bone.updateRotation(initial.getRotX(), initial.getRotY(), initial.getRotZ());
            bone.updatePosition(initial.getOffsetX(), initial.getOffsetY(), initial.getOffsetZ());
            bone.updateScale(initial.getScaleX(), initial.getScaleY(), initial.getScaleZ());
        }
        try {
            //? if forge {
            /*super.renderRecursively(poseStack, entity, bone, renderType, buffers, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha);
            *///?} else {
            super.renderRecursively(poseStack, entity, bone, renderType, buffers, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, packedColor);
            //?}
        } finally {
            if (previous != null) {
                bone.updateRotation(previous.getRotX(), previous.getRotY(), previous.getRotZ());
                bone.updatePosition(previous.getOffsetX(), previous.getOffsetY(), previous.getOffsetZ());
                bone.updateScale(previous.getScaleX(), previous.getScaleY(), previous.getScaleZ());
            }
        }
    }

    @Override
    //? if forge {
    /*public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer,
                                  int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
    *///?} else {
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer,
                                  int packedLight, int packedOverlay, int packedColor) {
    //?}
        if (!building) {
            //? if forge {
            /*super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            *///?} else {
            super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, packedColor);
            //?}
            return;
        }
        if (bone.isHidden()) {
            return;
        }

        List<GeoCube> cubes = bone.getCubes();
        for (int index = 0; index < cubes.size(); index++) {
            StructureSections.Section section = sectionOf(bone.getName(), index);
            if (section == null) {
                continue;
            }
            VertexConsumer target;
            int color;
            if (builtSections.contains(section.name())) {
                target = bufferSource.getBuffer(builtType);
                //? if forge {
                /*color = packColor(red, green, blue, alpha);
                *///?} else {
                color = packedColor;
                //?}
            } else if (activeType != null && section.name().equals(activeSection)) {
                target = bufferSource.getBuffer(activeType);
                color = packColor(activePulseHigh, activePulseLow, 0.0F, 1.0F);
            } else if (ghostType != null) {
                target = bufferSource.getBuffer(ghostType);
                color = packColor(GHOST_TINT.getRedFloat(), GHOST_TINT.getGreenFloat(), GHOST_TINT.getBlueFloat(),
                        GHOST_ALPHA);
            } else {
                continue;
            }
            poseStack.pushPose();
            //? if forge {
            /*renderCube(poseStack, cubes.get(index), target, packedLight, packedOverlay,
                    (color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F,
                    (color >>> 24) / 255.0F);
            *///?} else {
            renderCube(poseStack, cubes.get(index), target, packedLight, packedOverlay, color);
            //?}
            poseStack.popPose();
        }
    }

    private static int packColor(float red, float green, float blue, float alpha) {
        int a = Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        int r = Math.round(Mth.clamp(red, 0.0F, 1.0F) * 255.0F);
        int g = Math.round(Mth.clamp(green, 0.0F, 1.0F) * 255.0F);
        int b = Math.round(Mth.clamp(blue, 0.0F, 1.0F) * 255.0F);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private void renderStageSign(T entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ConstructionStatus status = entity.constructionStatus();
        BlueprintDefinition recipe = BlueprintDefinitions.get(status.blueprintId());
        if (recipe == null || recipe.stages().isEmpty()) {
            return;
        }
        int stageIndex = Math.min(status.builtStages(), recipe.stageCount() - 1);
        BlueprintDefinition.Stage stage = recipe.stages().get(stageIndex);

        Component line = Component.empty()
                .append(BlueprintConstructionVisuals.stageName(status.blueprintId(), stage.section()))
                .append(Component.literal(String.format("  %d/%d  ·  %d/%d",
                        status.hits(), Math.max(1, status.stageHits()), stageIndex + 1, recipe.stageCount())));

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + 0.7D, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        Font font = getFont();
        float x = -font.width(line) / 2.0F;
        font.drawInBatch(line, x, 0.0F, 0xFFE8DFC8, false, poseStack.last().pose(), buffer,
                Font.DisplayMode.NORMAL, 0x40000000, packedLight);
        poseStack.popPose();
    }

    private StructureSections.Section sectionOf(String bone, int cube) {
        for (StructureSections.Section section : sections.sections()) {
            if (section.contains(bone, cube)) {
                return section;
            }
        }
        return null;
    }

    /** The markup of the build's model, or null to draw it finished when the markup does not match the model. */
    private StructureSections sectionsFor(T entity) {
        ResourceLocation model = getGeoModel().getModelResource(entity);
        ResourceLocation markup = entity.constructionModel();
        if (markup == null) {
            String path = model.getPath();
            String name = path.substring(path.lastIndexOf('/') + 1);
            if (name.endsWith(".geo.json")) {
                name = name.substring(0, name.length() - ".geo.json".length());
            }
            markup = ResourceIds.id(model.getNamespace(), name);
        }
        StructureSections found = BlueprintConstructionVisuals.sections(markup);
        if (found == null || matchesLoadedModel(found, model)) {
            return found;
        }
        if (WARNED.add(model)) {
            LOGGER.warn("Build sections for {} do not match the loaded model; drawing it finished", model);
        }
        return null;
    }

    private boolean matchesLoadedModel(StructureSections found, ResourceLocation model) {
        Map<String, Integer> loaded = new HashMap<>();
        for (GeoBone bone : getGeoModel().getBakedModel(model).topLevelBones()) {
            countCubes(bone, loaded);
        }
        return found.matches(loaded);
    }

    private static void countCubes(GeoBone bone, Map<String, Integer> counts) {
        if (!bone.getCubes().isEmpty()) {
            counts.put(bone.getName(), bone.getCubes().size());
        }
        for (GeoBone child : bone.getChildBones()) {
            countCubes(child, counts);
        }
    }
}
