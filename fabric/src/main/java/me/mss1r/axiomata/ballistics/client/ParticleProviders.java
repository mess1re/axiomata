package me.mss1r.axiomata.ballistics.client;

import me.mss1r.axiomata.ballistics.client.particle.*;
import me.mss1r.axiomata.ballistics.particle.ParticleSet;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

/** Register client providers for a mod's particle IDs on Fabric. */
public final class ParticleProviders {
    private ParticleProviders() {}
    public static void register(ParticleSet particles) {
        var registry = ParticleFactoryRegistry.getInstance();
        registry.register(particles.SMOKE.get(), sprites -> new SmokeParticle.Provider(sprites, false));
        registry.register(particles.HEAVY_SMOKE.get(), sprites -> new SmokeParticle.Provider(sprites, true));
        registry.register(particles.MUZZLE_PLUME.get(), new MuzzlePlumeParticle.Provider(particles));
        registry.register(particles.IMPACT_SMOKE_PLUME.get(), new ImpactSmokePlumeParticle.Provider(particles));
        registry.register(particles.FRAGMENT.get(), new FragmentParticle.Provider());
    }
}
