package me.mss1r.axiomata.forge;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import me.mss1r.axiomata.ballistics.BallisticsModule;
import me.mss1r.axiomata.ballistics.ProjectilePassThroughControl;
import me.mss1r.axiomata.ballistics.ProjectilePassageHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.EntityHitResult;

public final class AxiomataEvents {
    private AxiomataEvents() {}
    public static void registerBlueprint(IEventBus modBus, IEventBus gameBus) {
        gameBus.addListener(AxiomataEvents::syncDatapacks);
        gameBus.addListener(AxiomataEvents::startTracking);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> Client.registerBlueprint(modBus, gameBus));
    }
    public static void registerBallistics(IEventBus modBus, IEventBus gameBus) {
        gameBus.addListener(AxiomataEvents::projectileImpact);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> modBus.addListener(Client::particles));
    }
    private static void syncDatapacks(OnDatapackSyncEvent event) {
        me.mss1r.axiomata.blueprint.internal.definition.BlueprintDefinitionCatalog.syncToClients(event.getPlayer(), event.getPlayerList());
        me.mss1r.axiomata.blueprint.tracing.OutlineCatalog.syncToClients(event.getPlayer(), event.getPlayerList());
        me.mss1r.axiomata.blueprint.internal.construction.ConstructionMarkupCatalog.syncToClients(event.getPlayer(), event.getPlayerList());
    }
    private static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player)
            me.mss1r.axiomata.blueprint.internal.construction.ConstructionSync.onStartTracking(event);
    }
    private static void projectileImpact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityHitResult hit
                && hit.getEntity() instanceof ProjectilePassThroughControl target
                && ProjectilePassageHandler.allowsPassage(target, event.getProjectile().position(),
                    event.getProjectile().getDeltaMovement(), hit.getLocation())) {
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
        }
    }
    private static final class Client {
        static void registerBlueprint(IEventBus modBus, IEventBus gameBus) {
            modBus.addListener(me.mss1r.axiomata.blueprint.client.events.BlueprintClientModEvents::clientSetup);
            modBus.addListener(me.mss1r.axiomata.blueprint.client.events.BlueprintClientModEvents::registerShaders);
            gameBus.addListener(Client::render);
            gameBus.addListener(me.mss1r.axiomata.blueprint.client.events.BlueprintClientGameEvents::onRenderGui);
        }
        static void particles(RegisterParticleProvidersEvent event) {
            me.mss1r.axiomata.ballistics.client.ParticleProviders.register(event, BallisticsModule.PARTICLES);
            event.registerSpriteSet(BallisticsModule.MUZZLE_CORE.get(),
                me.mss1r.axiomata.ballistics.client.particle.MuzzleCoreParticle.Provider::new);
        }
        static void render(RenderLevelStageEvent event) {
            me.mss1r.axiomata.blueprint.client.events.BlueprintClientGameEvents.onRenderLevelStage(event);
            me.mss1r.axiomata.collision.client.StructureCollisionDebugRenderer.renderCollision(event);
        }
    }
}
