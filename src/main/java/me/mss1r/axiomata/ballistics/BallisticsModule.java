package me.mss1r.axiomata.ballistics;

import dev.architectury.event.events.common.TickEvent;
import me.mss1r.axiomata.Axiomata;
import me.mss1r.axiomata.ballistics.damage.StructuralDamageSystem;
import me.mss1r.axiomata.ballistics.particle.ParticleSet;
//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLEnvironment;
*///?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
//?}

public final class BallisticsModule {
    public static final ParticleSet PARTICLES = new ParticleSet(Axiomata.MOD_ID);

    private BallisticsModule() {}

    public static void initialize(IEventBus modEventBus) {
        PARTICLES.register();
        TickEvent.SERVER_POST.register(server -> {
            for (var level : server.getAllLevels()) {
                StructuralDamageSystem.tick(level);
            }
            DistantFlight.tick(server);
        });
        if (FMLEnvironment.dist == Dist.CLIENT) {
            initializeClient(modEventBus);
        }
    }

    private static void initializeClient(IEventBus modEventBus) {
        modEventBus.addListener((RegisterParticleProvidersEvent event) ->
                me.mss1r.axiomata.ballistics.client.ParticleProviders.register(event, PARTICLES));
    }
}
