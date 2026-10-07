package me.mss1r.axiomata.ballistics.client;

import me.mss1r.axiomata.ballistics.client.particle.FragmentParticle;
import me.mss1r.axiomata.ballistics.client.particle.ImpactSmokePlumeParticle;
import me.mss1r.axiomata.ballistics.client.particle.MuzzlePlumeParticle;
import me.mss1r.axiomata.ballistics.client.particle.SmokeParticle;
import me.mss1r.axiomata.ballistics.particle.ParticleSet;
//? if forge {
/*import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
*///?} else {
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
//?}

public final class ParticleProviders {
    private ParticleProviders() {}

    /** Register on the consuming mod's particle-provider event if it supplies its own IDs. */
    public static void register(RegisterParticleProvidersEvent event, ParticleSet particles) {
        event.registerSpriteSet(particles.SMOKE.get(),
                sprites -> new SmokeParticle.Provider(sprites, false));
        event.registerSpriteSet(particles.HEAVY_SMOKE.get(),
                sprites -> new SmokeParticle.Provider(sprites, true));
        event.registerSpecial(particles.MUZZLE_PLUME.get(), new MuzzlePlumeParticle.Provider(particles));
        event.registerSpecial(particles.IMPACT_SMOKE_PLUME.get(), new ImpactSmokePlumeParticle.Provider(particles));
        event.registerSpecial(particles.FRAGMENT.get(), new FragmentParticle.Provider());
    }
}
