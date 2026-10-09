package me.mss1r.axiomata.ballistics;

import dev.architectury.event.events.common.TickEvent;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import me.mss1r.axiomata.Axiomata;
import me.mss1r.axiomata.ballistics.damage.StructuralDamageSystem;
import me.mss1r.axiomata.ballistics.particle.ParticleSet;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
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
    private static final DeferredRegister<ParticleType<?>> EFFECT_PARTICLES =
            DeferredRegister.create(Axiomata.MOD_ID, Registries.PARTICLE_TYPE);
    public static final RegistrySupplier<SimpleParticleType> MUZZLE_CORE =
            EFFECT_PARTICLES.register("muzzle_core", () -> new SimpleParticleType(false));

    private BallisticsModule() {}

    public static void initialize(IEventBus modEventBus, IEventBus gameEventBus) {
        PARTICLES.register();
        EFFECT_PARTICLES.register();
        ProjectilePassageHandler.register(gameEventBus);
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
        modEventBus.addListener((RegisterParticleProvidersEvent event) -> {
            me.mss1r.axiomata.ballistics.client.ParticleProviders.register(event, PARTICLES);
            event.registerSpriteSet(MUZZLE_CORE.get(),
                    me.mss1r.axiomata.ballistics.client.particle.MuzzleCoreParticle.Provider::new);
        });
    }
}
