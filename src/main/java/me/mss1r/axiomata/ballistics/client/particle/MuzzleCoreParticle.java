package me.mss1r.axiomata.ballistics.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/** Brief full-bright core. Velocity encodes the plume direction and scale, as for the smoke emitter. */
public final class MuzzleCoreParticle extends TextureSheetParticle {
    private final float radius;

    private MuzzleCoreParticle(ClientLevel level, double x, double y, double z,
                               double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z);
        Vec3 encoded = new Vec3(dx, dy, dz);
        float scale = Mth.clamp((float) encoded.length(), .75F, 16F);
        Vec3 velocity = encoded.normalize().scale(scale * .035);
        this.xd = velocity.x; this.yd = velocity.y; this.zd = velocity.z;
        this.radius = scale * .2F;
        this.lifetime = 4;
        this.hasPhysics = false;
        this.friction = .9F;
        this.alpha = .95F;
        this.setSprite(sprites.get(0, 0));
        this.setColor(1, .95F, .72F);
    }

    @Override public void tick() {
        super.tick();
        float progress = (float) this.age / this.lifetime;
        this.alpha = .95F * (1 - progress * progress);
        this.gCol = Mth.lerp(progress, .95F, .65F);
        this.bCol = Mth.lerp(progress, .72F, .18F);
    }

    @Override public float getQuadSize(float partial) {
        float progress = Mth.clamp((this.age + partial) / this.lifetime, 0, 1);
        return radius * Mth.lerp(progress, .85F, 1.15F);
    }
    @Override public int getLightColor(float partial) { return 0xF000F0; }
    @Override public @NotNull ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                                  double x, double y, double z, double dx, double dy, double dz) {
            return new MuzzleCoreParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
