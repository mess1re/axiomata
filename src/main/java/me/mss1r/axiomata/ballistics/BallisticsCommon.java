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
public final class BallisticsCommon {
    public static final ParticleSet PARTICLES = new ParticleSet(Axiomata.MOD_ID);
    private static final DeferredRegister<ParticleType<?>> EFFECT_PARTICLES =
            DeferredRegister.create(Axiomata.MOD_ID, Registries.PARTICLE_TYPE);
    public static final RegistrySupplier<SimpleParticleType> MUZZLE_CORE =
            EFFECT_PARTICLES.register("muzzle_core", () -> new SimpleParticleType(false) {});

    private BallisticsCommon() {}

    public static void initialize() {
        me.mss1r.axiomata.ballistics.profile.ImpactProfiles.initialize();
        PARTICLES.register();
        EFFECT_PARTICLES.register();
        TickEvent.SERVER_POST.register(server -> {
            for (var level : server.getAllLevels()) {
                StructuralDamageSystem.tick(level);
                FragmentFlight.tick(level);
            }
            DistantFlight.tick(server);
        });
        dev.architectury.event.events.common.LifecycleEvent.SERVER_STOPPED.register(server -> FragmentFlight.clear());
    }
}
