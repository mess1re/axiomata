package me.mss1r.axiomata.ballistics;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.particles.SimpleParticleType;
import me.mss1r.axiomata.ballistics.particle.ParticleSet;
/** Portable particle handles retain the same names on every loader. */
public final class BallisticsModule {
    public static final ParticleSet PARTICLES = BallisticsCommon.PARTICLES;
    public static final RegistrySupplier<SimpleParticleType> MUZZLE_CORE = BallisticsCommon.MUZZLE_CORE;
    private BallisticsModule() {}
    public static void initialize() { BallisticsCommon.initialize(); }
}
