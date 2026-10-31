package me.mss1r.axiomata.fabric;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.architectury.registry.menu.MenuRegistry;
import me.mss1r.axiomata.Axiomata;
import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.ballistics.BallisticsCommon;
import me.mss1r.axiomata.ballistics.client.ParticleProviders;
import me.mss1r.axiomata.ballistics.client.particle.MuzzleCoreParticle;
import me.mss1r.axiomata.blueprint.client.events.BlueprintPlacementPreview;
import me.mss1r.axiomata.blueprint.client.renderer.ConstructionHighlightRenderType;
import me.mss1r.axiomata.blueprint.client.screen.BlueprintUseScreen;
import me.mss1r.axiomata.blueprint.client.screen.DrawingTableScreen;
import me.mss1r.axiomata.blueprint.registry.BlueprintBlocks;
import me.mss1r.axiomata.blueprint.registry.BlueprintMenus;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.client.StructureCollisionDebugDraw;
import me.mss1r.axiomata.collision.system.StructureCollisionSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.RenderType;

public final class AxiomataFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        me.mss1r.axiomata.client.AxiomataUpdateNotifier.register();
        BlockRenderLayerMap.INSTANCE.putBlock(BlueprintBlocks.DRAWING_TABLE.get(), RenderType.cutout());
        MenuRegistry.registerScreenFactory(BlueprintMenus.DRAWING_TABLE_MENU.get(), DrawingTableScreen::new);
        MenuRegistry.registerScreenFactory(BlueprintMenus.BLUEPRINT_USE_MENU.get(), BlueprintUseScreen::new);
        ParticleProviders.register(BallisticsCommon.PARTICLES);
        ParticleFactoryRegistry.getInstance().register(BallisticsCommon.MUZZLE_CORE.get(), MuzzleCoreParticle.Provider::new);
        CoreShaderRegistrationCallback.EVENT.register(context -> context.register(
                ResourceIds.id(Axiomata.MOD_ID, "construction_highlight"), DefaultVertexFormat.NEW_ENTITY,
                ConstructionHighlightRenderType::setShader));
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof CollidableStructure structure) StructureCollisionSystem.register(structure);
        });
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof CollidableStructure structure) StructureCollisionSystem.unregister(structure);
        });
        WorldRenderEvents.START.register(context ->
                me.mss1r.axiomata.blueprint.client.renderer.ConstructionStageSigns.beginFrame());
        WorldRenderEvents.LAST.register(context ->
                me.mss1r.axiomata.blueprint.client.renderer.ConstructionStageSigns.render());
        WorldRenderEvents.AFTER_ENTITIES.register(context ->
                StructureCollisionDebugDraw.renderCollision(context.matrixStack(), context.camera().getPosition()));
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            //? if >=1.21 {
            float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(false);
            //?} else {
            /*float partialTick = context.tickDelta();
            *///?}
            BlueprintPlacementPreview.render(context.matrixStack(), context.camera().getPosition(), partialTick);
        });
    }
}
