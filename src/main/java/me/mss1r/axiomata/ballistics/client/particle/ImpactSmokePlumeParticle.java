package me.mss1r.axiomata.ballistics.client.particle;

import me.mss1r.axiomata.ballistics.particle.ParticleSet;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class ImpactSmokePlumeParticle extends NoRenderParticle {
    private static final Vec3 DEFAULT_NORMAL = new Vec3(0.0D, 1.0D, 0.0D);

    private final boolean heavy;
    private final float scale;
    private final ParticleSet particles;
    private final Vec3 normal;
    private final Vec3 right;
    private final Vec3 up;
    private final int totalSmoke;
    private int emittedSmoke;

    private ImpactSmokePlumeParticle(ParticleSet particles, ClientLevel level, double x, double y, double z,
                                     double encodedX, double encodedY, double encodedZ) {
        super(level, x, y, z);
        this.particles = particles;
        Vec3 encoded = new Vec3(encodedX, encodedY, encodedZ);
        double encodedLength = encoded.length();
        this.heavy = encodedLength > 3.5D;
        this.scale = Mth.clamp((float) (this.heavy ? encodedLength - 4.0D : encodedLength),
                0.65F, 2.8F);
        this.normal = encodedLength > 1.0E-8D ? encoded.normalize() : DEFAULT_NORMAL;
        Vec3 referenceUp = Math.abs(this.normal.y) > 0.95D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : DEFAULT_NORMAL;
        this.right = this.normal.cross(referenceUp).normalize();
        this.up = this.right.cross(this.normal).normalize();
        this.totalSmoke = this.heavy
                ? Mth.clamp(Mth.ceil(24.0F + this.scale * 8.0F), 30, 47)
                : Mth.clamp(Mth.ceil(8.0F + this.scale * 5.0F), 12, 20);
        this.lifetime = 3;
    }

    @Override
    public void tick() {
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        int remainingSmoke = this.totalSmoke - this.emittedSmoke;
        int remainingTicks = Math.max(1, this.lifetime - this.age);
        int count = this.age == 0
                ? Math.min(remainingSmoke, Mth.ceil(this.totalSmoke * 0.65F))
                : Mth.ceil((float) remainingSmoke / (float) remainingTicks);
        float progress = this.lifetime <= 1 ? 1.0F : (float) this.age / (float) (this.lifetime - 1);
        SimpleParticleType smoke = this.heavy
                ? particles.HEAVY_SMOKE.get()
                : particles.SMOKE.get();

        for (int i = 0; i < count; i++) {
            double angle = this.random.nextDouble() * Mth.TWO_PI;
            Vec3 radialDirection = this.right.scale(Math.cos(angle))
                    .add(this.up.scale(Math.sin(angle)));
            double radius = this.scale * (0.2D + progress * 0.12D)
                    * Math.sqrt(this.random.nextDouble());
            Vec3 position = new Vec3(this.x, this.y, this.z)
                    .add(radialDirection.scale(radius))
                    .add(this.normal.scale(this.scale
                            * (0.02D + this.random.nextDouble() * 0.045D)));
            double horizontalSpeed = this.scale * (0.01D + this.random.nextDouble() * 0.024D);
            double outwardSpeed = 0.022D + this.random.nextDouble() * 0.04D
                    + (this.heavy ? 0.008D : 0.003D);
            Vec3 velocity = radialDirection.scale(horizontalSpeed)
                    .add(this.normal.scale(outwardSpeed))
                    .add(0.0D, 0.008D + this.random.nextDouble() * 0.018D, 0.0D);
            this.level.addParticle(smoke, true, position.x, position.y, position.z,
                    velocity.x, velocity.y, velocity.z);
        }
        this.emittedSmoke += count;

        super.tick();
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final ParticleSet particles;

        public Provider(ParticleSet particles) {
            this.particles = particles;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z, double xd, double yd, double zd) {
            return new ImpactSmokePlumeParticle(particles, level, x, y, z, xd, yd, zd);
        }
    }
}
