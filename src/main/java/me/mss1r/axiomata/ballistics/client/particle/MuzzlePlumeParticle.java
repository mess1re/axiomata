package me.mss1r.axiomata.ballistics.client.particle;

import me.mss1r.axiomata.ballistics.particle.ParticleSet;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class MuzzlePlumeParticle extends NoRenderParticle {
    private static final Vec3 DEFAULT_DIRECTION = new Vec3(0.0D, 0.0D, 1.0D);

    private final ParticleSet particles;
    private final Vec3 direction;
    private final Vec3 right;
    private final Vec3 up;
    private final float scale;
    private final int totalSmoke;
    private int emittedSmoke;
    private final double lobePhase;

    private MuzzlePlumeParticle(ParticleSet particles, ClientLevel level, double x, double y, double z,
                                double dx, double dy, double dz) {
        super(level, x, y, z);
        this.particles = particles;
        Vec3 encodedDirection = new Vec3(dx, dy, dz);
        this.scale = Mth.clamp((float) encodedDirection.length(), 0.75F, 16F);
        this.direction = encodedDirection.lengthSqr() > 1.0E-8D
                ? encodedDirection.normalize()
                : DEFAULT_DIRECTION;
        Vec3 referenceUp = Math.abs(this.direction.y) > 0.95D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(0.0D, 1.0D, 0.0D);
        this.right = this.direction.cross(referenceUp).normalize();
        this.up = this.right.cross(this.direction).normalize();
        float ordinaryScale = Math.min(this.scale, 2.5F);
        this.totalSmoke = Mth.clamp(Mth.ceil(4.0F + ordinaryScale * 20.0F), 18, 44);
        this.lifetime = this.scale > 2.5F ? 14 : Mth.clamp(Mth.ceil(3.0F + this.scale * 2.0F), 5, 16);
        this.lobePhase = this.scale > 2.5F ? this.random.nextDouble() * Mth.TWO_PI : 0;
    }

    @Override
    public void tick() {
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        if (this.scale > 2.5F) {
            emitLargeSmoke();
            if (this.age < 4) emitFlash();
            super.tick();
            return;
        }

        int remainingSmoke = this.totalSmoke - this.emittedSmoke;
        int remainingTicks = Math.max(1, this.lifetime - this.age);
        int count = this.age == 0
                ? Math.min(remainingSmoke, Mth.ceil(this.totalSmoke * 0.3F))
                : Mth.ceil((float) remainingSmoke / (float) remainingTicks);
        SimpleParticleType smoke = particles.SMOKE.get();

        for (int i = 0; i < count; i++) {
            double angle = this.random.nextDouble() * Mth.TWO_PI;
            Vec3 radialDirection = this.right.scale(Math.cos(angle))
                    .add(this.up.scale(Math.sin(angle)));
            double radius = this.scale * (0.006D + this.random.nextDouble() * 0.018D);
            Vec3 radialOffset = radialDirection.scale(radius);
            double distance = this.scale * (0.012D + this.random.nextDouble() * 0.035D);
            Vec3 position = new Vec3(this.x, this.y, this.z)
                    .add(this.direction.scale(distance))
                    .add(radialOffset);
            double forwardSpeed = (0.16D + this.random.nextDouble() * 0.16D) * (0.9D + this.scale * 0.16D);
            double coneSpeed = this.scale * (0.006D + this.random.nextDouble() * 0.018D);
            Vec3 velocity = this.direction.scale(forwardSpeed)
                    .add(radialDirection.scale(coneSpeed))
                    .add(0.0D, this.random.nextDouble() * 0.006D, 0.0D);
            this.level.addParticle(smoke, true, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
        }
        this.emittedSmoke += count;
        super.tick();
    }

    private void emitLargeSmoke() {
        Vec3 muzzle = new Vec3(this.x, this.y, this.z);
        // The collar, slower inner gas and axial plume share a continuous velocity range.
        if (this.age == 0) {
            int ringCount = Mth.ceil(this.scale * 7);
            for (int i = 0; i < ringCount; i++) {
                double angle = this.random.nextDouble() * Mth.TWO_PI;
                Vec3 radial = this.right.scale(Math.cos(angle)).add(this.up.scale(Math.sin(angle)));
                double fraction = i % 3 == 0 ? Math.sqrt(this.random.nextDouble()) * .65D
                        : .45D + this.random.nextDouble() * .55D;
                double lobe = 1 + .22D * Math.sin(angle * 3 + this.lobePhase);
                Vec3 position = muzzle.add(this.direction.scale(this.scale * (0.02D + this.random.nextDouble() * 0.08D)))
                        .add(radial.scale(this.scale * .18D * fraction * lobe));
                Vec3 velocity = radial.scale(this.scale * (0.008D + fraction * .055D) * lobe)
                        .add(this.direction.scale(this.scale * (0.005D + this.random.nextDouble() * 0.008D)));
                smokePuff(position, velocity);
            }
        }
        if (this.age >= 5) {
            for (int i = 0; i < 4; i++) {
                Vec3 position = muzzle.add(this.direction.scale(this.scale * (.01D + this.random.nextDouble() * .05D)));
                Vec3 velocity = this.direction.scale(this.scale * (.005D + this.random.nextDouble() * .015D));
                smokePuff(position, velocity, .35F);
            }
            return;
        }
        int count = Mth.ceil(this.scale * switch (this.age) { case 0 -> 10; case 1 -> 6; case 2 -> 4; case 3 -> 2; default -> 1; });
        for (int i = 0; i < count; i++) {
            double depth = Math.pow(this.random.nextDouble(), 1.5D);
            double angle = this.random.nextDouble() * Mth.TWO_PI;
            Vec3 radial = this.right.scale(Math.cos(angle)).add(this.up.scale(Math.sin(angle)));
            double radius = this.scale * (0.035D + depth * 0.14D) * Math.sqrt(this.random.nextDouble());
            Vec3 position = muzzle.add(this.direction.scale(this.scale * (0.06D + depth * 0.6D)))
                    .add(radial.scale(radius));
            Vec3 velocity = this.direction.scale(this.scale * (0.015D + depth * .085D + this.random.nextDouble() * .03D))
                    .add(radial.scale(this.scale * (0.006D + this.random.nextDouble() * 0.028D)));
            smokePuff(position, velocity);
        }
    }

    private void smokePuff(Vec3 position, Vec3 velocity) {
        smokePuff(position, velocity, 1);
    }

    private void smokePuff(Vec3 position, Vec3 velocity, float size) {
        var puff = Minecraft.getInstance().particleEngine.createParticle(particles.SMOKE.get(),
                position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
        if (puff != null) {
            puff.scale(this.scale / 2.5F * size);
            puff.setColor(0.42F, 0.41F, 0.39F);
        }
    }

    private void emitFlash() {
        if (this.age < 2) {
            for (int i = 0; i < 4; i++) {
                Vec3 position = new Vec3(this.x, this.y, this.z).add(this.direction.scale(this.scale * (.06D + i * .16D)));
                Vec3 encoded = this.direction.scale(this.scale * (1 - i * .1D));
                Minecraft.getInstance().particleEngine.createParticle(particles.MUZZLE_CORE.get(),
                        position.x, position.y, position.z, encoded.x, encoded.y, encoded.z);
            }
        }
        // Flash density is independent of the number of smoke puffs.
        int count = Mth.ceil(this.scale * (24 - this.age * 6));
        for (int i = 0; i < count; i++) {
            double angle = this.random.nextDouble() * Mth.TWO_PI;
            Vec3 radial = this.right.scale(Math.cos(angle)).add(this.up.scale(Math.sin(angle)));
            double depth = this.random.nextDouble() < .7D ? this.random.nextDouble() * .35D : this.random.nextDouble();
            double distance = this.scale * (0.02D + depth * 0.5D);
            double radius = this.scale * (0.035D + depth * 0.12D) * Math.sqrt(this.random.nextDouble());
            Vec3 position = new Vec3(this.x, this.y, this.z).add(this.direction.scale(distance)).add(radial.scale(radius));
            Vec3 velocity = this.direction.scale(this.scale * (0.045D + this.random.nextDouble() * 0.05D))
                    .add(radial.scale(this.scale * (0.015D + this.random.nextDouble() * 0.025D)));
            var flame = Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.FLAME,
                    position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
            if (flame != null) {
                flame.scale(this.scale * 0.7F);
                flame.setLifetime(8 + this.random.nextInt(6));
            }
        }
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final ParticleSet particles;

        public Provider(ParticleSet particles) {
            this.particles = particles;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z, double xd, double yd, double zd) {
            return new MuzzlePlumeParticle(particles, level, x, y, z, xd, yd, zd);
        }
    }
}
