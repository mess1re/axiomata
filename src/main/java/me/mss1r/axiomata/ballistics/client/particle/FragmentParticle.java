package me.mss1r.axiomata.ballistics.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Restores the supplied fragment velocity after TerrainParticle replaces it with a random one.
 */
public final class FragmentParticle extends TerrainParticle {
    private static final int SHORTEST_LIFE_TICKS = 30;
    private static final int LIFE_SPREAD_TICKS = 30;

    private FragmentParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                             BlockState state) {
        super(level, x, y, z, xd, yd, zd, state);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.quadSize *= 1.5F + this.random.nextFloat() * 1.5F;
        this.lifetime = SHORTEST_LIFE_TICKS + this.random.nextInt(LIFE_SPREAD_TICKS);
    }

    public static final class Provider implements ParticleProvider<BlockParticleOption> {
        @Override
        @Nullable
        public Particle createParticle(BlockParticleOption options, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            BlockState state = options.getState();
            return state.isAir() || state.is(Blocks.MOVING_PISTON)
                    ? null
                    : new FragmentParticle(level, x, y, z, xd, yd, zd, state);
        }
    }
}
