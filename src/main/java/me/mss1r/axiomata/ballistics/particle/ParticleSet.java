package me.mss1r.axiomata.ballistics.particle;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
//? if forge {
/*import com.mojang.serialization.Codec;
*///?} else {
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
//?}
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

/** Custom particle IDs. Consumers supply the sprite lists for their smoke and heavy-smoke IDs. */
public final class ParticleSet {
    private final DeferredRegister<ParticleType<?>> particleTypes;

    public final RegistrySupplier<SimpleParticleType> SMOKE;
    public final RegistrySupplier<SimpleParticleType> HEAVY_SMOKE;
    public final RegistrySupplier<SimpleParticleType> MUZZLE_PLUME;
    public final RegistrySupplier<SimpleParticleType> IMPACT_SMOKE_PLUME;

    public final RegistrySupplier<ParticleType<BlockParticleOption>> FRAGMENT;

    public ParticleSet(String namespace) {
        this(namespace, "smoke", "heavy_smoke");
    }

    public ParticleSet(String namespace, String smokePath, String heavySmokePath) {
        particleTypes = DeferredRegister.create(namespace, Registries.PARTICLE_TYPE);
        SMOKE =
            particleTypes.register(smokePath, () -> new SimpleParticleType(false));
        HEAVY_SMOKE =
            particleTypes.register(heavySmokePath, () -> new SimpleParticleType(false));
        MUZZLE_PLUME =
            particleTypes.register("muzzle_plume", () -> new SimpleParticleType(false));
        IMPACT_SMOKE_PLUME =
            particleTypes.register("impact_smoke_plume", () -> new SimpleParticleType(false));
        //? if forge {
        /*FRAGMENT = particleTypes.register("fragment", FragmentParticleType::new);
        *///?} else {
        FRAGMENT =
            particleTypes.register("fragment", () -> new ParticleType<BlockParticleOption>(false) {
                @Override
                public MapCodec<BlockParticleOption> codec() {
                    return BlockParticleOption.codec(this);
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, BlockParticleOption> streamCodec() {
                    return BlockParticleOption.streamCodec(this);
                }
            });
        //?}
    }

    //? if forge {
    /*private static final class FragmentParticleType extends ParticleType<BlockParticleOption> {
        private FragmentParticleType() {
            super(false, BlockParticleOption.DESERIALIZER);
        }

        @Override
        public Codec<BlockParticleOption> codec() {
            return BlockParticleOption.codec(this);
        }
    }
    *///?}

    public void register() {
        particleTypes.register();
    }
}
