package me.mss1r.axiomata.ballistics.client.particle;

import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MuzzleCoreParticleTest {
    // Construction and non-colliding ticks need neither a world nor a rendered texture.
    private static final SpriteSet ONE_FRAME = new SpriteSet() {
        @Override public TextureAtlasSprite get(int age, int lifetime) {
            int index = age * (1 - 1) / lifetime;
            assertEquals(0, index);
            return null;
        }
        @Override public TextureAtlasSprite get(RandomSource random) { return null; }
    };

    @Test
    void singleFrameCoreCanBeCreatedAndTickedThroughItsLifetime() {
        var particle = assertDoesNotThrow(() -> (MuzzleCoreParticle) new MuzzleCoreParticle.Provider(ONE_FRAME)
                .createParticle(null, null, 0, 0, 0, 0, 16, 0));
        assertEquals(4, particle.getLifetime());
        for (int tick = 0; tick < particle.getLifetime(); tick++) {
            assertTrue(particle.isAlive());
            assertTrue(Float.isFinite(particle.getQuadSize(.5F)));
            assertEquals(0xF000F0, particle.getLightColor(.5F));
            assertDoesNotThrow(particle::tick);
        }
        particle.tick();
        assertFalse(particle.isAlive());
    }

    @Test
    void zeroDirectionStillProducesAFiniteCore() {
        var particle = assertDoesNotThrow(() -> (MuzzleCoreParticle) new MuzzleCoreParticle.Provider(ONE_FRAME)
                .createParticle(null, null, 0, 0, 0, 0, 0, 0));
        assertTrue(Float.isFinite(particle.getQuadSize(0)));
        assertDoesNotThrow(particle::tick);
    }
}
