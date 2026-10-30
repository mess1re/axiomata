package me.mss1r.axiomata.ballistics;
import net.neoforged.bus.api.IEventBus;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.particles.SimpleParticleType;
import me.mss1r.axiomata.ballistics.particle.ParticleSet;
/** Loader-facing entry point retained for integrating mods. */
public final class BallisticsModule {
    public static final ParticleSet PARTICLES = BallisticsCommon.PARTICLES;
    public static final RegistrySupplier<SimpleParticleType> MUZZLE_CORE = BallisticsCommon.MUZZLE_CORE;
    private BallisticsModule() {}
    public static void initialize(IEventBus modBus, IEventBus gameBus) {
        BallisticsCommon.initialize();
        me.mss1r.axiomata.neoforge.AxiomataEvents.registerBallistics(modBus, gameBus);
    }
}
