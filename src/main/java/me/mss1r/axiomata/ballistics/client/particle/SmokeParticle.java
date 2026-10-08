package me.mss1r.axiomata.ballistics.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public final class SmokeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private float baseSize;
    private final float baseAlpha;
    private final float phase;
    private final float animationOffset;
    private final float buoyancy;
    private final float rollSpeed;

    private SmokeParticle(ClientLevel level, double x, double y, double z,
                               double xd, double yd, double zd, SpriteSet sprites, boolean heavy) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.baseSize = heavy
                ? 1.2F + this.random.nextFloat() * 0.7F
                : 0.72F + this.random.nextFloat() * 0.48F;
        this.baseAlpha = heavy
                ? 0.68F + this.random.nextFloat() * 0.08F
                : 0.58F + this.random.nextFloat() * 0.1F;
        this.phase = this.random.nextFloat() * Mth.TWO_PI;
        this.animationOffset = this.random.nextFloat() * 0.14F;
        this.buoyancy = heavy ? 0.0018F : 0.0012F;
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.rollSpeed = (this.random.nextFloat() - 0.5F) * (heavy ? 0.006F : 0.01F);
        float shade = heavy
                ? 0.68F + this.random.nextFloat() * 0.14F
                : 0.84F + this.random.nextFloat() * 0.12F;
        this.rCol = shade;
        this.gCol = shade * 0.99F;
        this.bCol = shade * 0.96F;
        this.lifetime = heavy
                ? 100 + this.random.nextInt(61)
                : 58 + this.random.nextInt(43);
        this.friction = heavy ? 0.965F : 0.955F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.xd = xd + (this.random.nextDouble() - 0.5D) * 0.012D;
        this.yd = yd + 0.006D + this.random.nextDouble() * 0.012D;
        this.zd = zd + (this.random.nextDouble() - 0.5D) * 0.012D;
        this.quadSize = this.baseSize * 0.82F;
        this.alpha = this.baseAlpha;
        this.updateSprite(this.animationOffset);
    }

    @Override
    public Particle scale(float factor) {
        this.baseSize *= factor;
        return super.scale(factor);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        float progress = Mth.clamp((float) this.age / (float) this.lifetime, 0.0F, 1.0F);
        double curl = Math.sin(this.phase + this.age * 0.105F) * 0.00042D;
        this.xd += curl;
        this.zd += Math.cos(this.phase + this.age * 0.09F) * 0.00042D;
        this.yd += this.buoyancy * (1.0F - progress * 0.55F);
        this.move(this.xd, this.yd, this.zd);
        this.xd *= this.friction;
        this.yd *= 0.982D;
        this.zd *= this.friction;
        this.roll += this.rollSpeed * (1.0F - progress * 0.65F);

        float growth = 1.0F - (1.0F - progress) * (1.0F - progress);
        this.quadSize = this.baseSize * Mth.lerp(growth, 0.82F, 1.2F);

        float fadeStart = 0.28F;
        float fadeProgress = Mth.clamp((progress - fadeStart) / (1.0F - fadeStart), 0.0F, 1.0F);
        float smoothFade = fadeProgress * fadeProgress * (3.0F - 2.0F * fadeProgress);
        this.alpha = this.baseAlpha * (1.0F - smoothFade);
        this.updateSprite(Mth.clamp(progress + this.animationOffset, 0.0F, 1.0F));
    }

    private void updateSprite(float progress) {
        int frame = Mth.clamp((int) Math.floor(progress * 8.0F), 0, 7);
        this.setSprite(this.sprites.get(frame, 7));
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final boolean heavy;

        public Provider(SpriteSet sprites, boolean heavy) {
            this.sprites = sprites;
            this.heavy = heavy;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z, double xd, double yd, double zd) {
            return new SmokeParticle(level, x, y, z, xd, yd, zd, this.sprites, this.heavy);
        }
    }
}
