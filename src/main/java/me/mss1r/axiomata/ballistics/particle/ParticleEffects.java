package me.mss1r.axiomata.ballistics.particle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class ParticleEffects {
    private static final Vec3 DEFAULT_DIRECTION = new Vec3(0.0D, 0.0D, 1.0D);

    public record MuzzleProfile(double scale, double range, int puffCount, int flameSteps,
                               int flamePoints, int emberCount) {}

    private final ParticleSet particles;

    public ParticleEffects(ParticleSet particles) {
        this.particles = java.util.Objects.requireNonNull(particles);
    }

    public void muzzleBlast(ServerLevel level, Vec3 origin, Vec3 direction, MuzzleProfile profile) {
        Vec3 forward = safeDirection(direction);
        Basis basis = basis(forward);
        double scale = profile.scale;

        sendLongRange(level, ParticleTypes.FLASH, origin, 1,
                Vec3.ZERO, 0.0D, profile.range);
        pressureCone(level, origin, forward, basis, profile);
        sendVelocity(level, particles.MUZZLE_PLUME.get(), origin,
                forward.scale(scale), profile.range);

        for (int step = 0; step < profile.flameSteps; step++) {
            double progress = profile.flameSteps <= 1
                    ? 0.0D : (double) step / (double) (profile.flameSteps - 1);
            double distance = scale * (0.055D + step * 0.135D);
            double radius = scale * (0.01D + step * 0.015D);
            int pointsAtStep = Math.max(2, (int) Math.ceil(profile.flamePoints
                    * (1.55D - progress * 0.7D)));
            Vec3 axis = origin.add(forward.scale(distance));
            for (int point = 0; point < pointsAtStep; point++) {
                double angle = level.random.nextDouble() * Math.PI * 2.0D;
                Vec3 radialDirection = basis.right.scale(Math.cos(angle))
                        .add(basis.up.scale(Math.sin(angle)));
                Vec3 radial = radialDirection.scale(radius * Math.sqrt(level.random.nextDouble()));
                Vec3 position = axis.add(radial);
                Vec3 velocity = forward.scale((0.13D + level.random.nextDouble() * 0.15D)
                                * (0.92D + scale * 0.12D))
                        .add(radialDirection.scale(scale
                                * (0.003D + level.random.nextDouble() * 0.012D)));
                sendVelocity(level, progress > 0.62D && level.random.nextFloat() < 0.2F
                                ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME,
                        position, velocity, profile.range);
            }
        }

        for (int ember = 0; ember < profile.emberCount; ember++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            Vec3 radialDirection = basis.right.scale(Math.cos(angle))
                    .add(basis.up.scale(Math.sin(angle)));
            Vec3 position = origin
                    .add(forward.scale(scale * (0.06D + level.random.nextDouble() * 0.16D)))
                    .add(radialDirection.scale(scale * level.random.nextDouble() * 0.035D));
            Vec3 velocity = forward.scale((0.34D + level.random.nextDouble() * 0.32D)
                            * (0.82D + scale * 0.2D))
                    .add(radialDirection.scale(scale
                            * (0.012D + level.random.nextDouble() * 0.035D)))
                    .add(0.0D, level.random.nextDouble() * 0.035D, 0.0D);
            sendVelocity(level, ember % 4 == 0 ? ParticleTypes.LAVA
                            : ember % 4 == 1 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME,
                    position, velocity, profile.range);
        }
    }

    public void rocketLaunch(ServerLevel level, Vec3 origin, Vec3 direction) {
        Vec3 forward = safeDirection(direction);
        Basis basis = basis(forward);
        sendLongRange(level, ParticleTypes.SMALL_FLAME, origin, 3,
                new Vec3(0.025D, 0.025D, 0.025D), 0.025D, 96.0D);
        sendLongRange(level, ParticleTypes.CLOUD, origin, 2,
                new Vec3(0.035D, 0.035D, 0.035D), 0.012D, 96.0D);
        sendLongRange(level, particles.SMOKE.get(), origin, 2,
                new Vec3(0.025D, 0.025D, 0.025D), 0.008D, 96.0D);

        for (int step = 0; step < 4; step++) {
            double distance = 0.08D + step * 0.14D;
            double radius = 0.01D + step * 0.012D;
            Vec3 axis = origin.add(forward.scale(distance));
            for (int point = 0; point < 2; point++) {
                double angle = Math.PI * (point + level.random.nextDouble() * 0.35D);
                Vec3 radial = basis.right.scale(Math.cos(angle) * radius)
                        .add(basis.up.scale(Math.sin(angle) * radius));
                Vec3 velocity = forward.scale(0.18D + level.random.nextDouble() * 0.07D)
                        .add(radial.scale(0.4D));
                sendVelocity(level, (point & 1) == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME,
                        axis.add(radial), velocity, 96.0D);
            }
            sendLongRange(level, step < 2 ? ParticleTypes.SMOKE : ParticleTypes.CLOUD,
                    axis, 1, new Vec3(radius, radius, radius), 0.008D, 96.0D);
        }
    }

    public void penetrationImpact(ServerLevel level, Vec3 impact, double radius) {
        penetrationImpact(level, impact, radius, new Vec3(0.0D, 1.0D, 0.0D));
    }

    public void penetrationImpact(ServerLevel level, Vec3 impact, double radius, Vec3 outwardNormal) {
        double scale = clamp(radius, 0.65D, 2.25D);
        radialDust(level, impact, 10 + (int) Math.round(scale * 4.0D), scale * 0.75D, 150.0D);
        sendVelocity(level, particles.IMPACT_SMOKE_PLUME.get(),
                impact.add(safeDirection(outwardNormal).scale(0.04D)),
                safeDirection(outwardNormal).scale(scale), 150.0D);
        blockDebris(level, impact, 24 + (int) Math.round(scale * 14.0D), scale * 0.26D, 150.0D);
    }

    public void impact(ServerLevel level, Vec3 position, float intensity, boolean heavy) {
        impact(level, position, intensity, heavy, new Vec3(0.0D, 1.0D, 0.0D));
    }

    public void impact(ServerLevel level, Vec3 position, float intensity,
                              boolean heavy, Vec3 outwardNormal) {
        double scale = clamp(0.7D + Math.sqrt(Math.max(0.0F, intensity)) * 0.35D,
                0.85D, heavy ? 2.8D : 1.9D);
        double range = heavy ? 240.0D : 160.0D;
        radialDust(level, position,
                heavy ? 28 + (int) (scale * 8.0D) : 13 + (int) (scale * 5.0D),
                heavy ? scale * 1.15D : scale * 0.8D, range);
        sendVelocity(level, particles.IMPACT_SMOKE_PLUME.get(),
                position.add(safeDirection(outwardNormal).scale(0.08D)),
                safeDirection(outwardNormal).scale(scale + (heavy ? 4.0D : 0.0D)), range);
        blockDebris(level, position, heavy ? 48 + (int) (scale * 16.0D) : 22 + (int) (scale * 10.0D),
                (heavy ? 0.42D : 0.3D) * scale, range);
    }

    public void scattershotImpact(ServerLevel level, Vec3 position,
                                         boolean stonePellet, Vec3 outwardNormal) {
        Vec3 normal = safeDirection(outwardNormal);
        Vec3 particlePosition = position.add(normal.scale(0.025D));
        Vec3 dustVelocity = normal.scale(0.035D + level.random.nextDouble() * 0.035D)
                .add((level.random.nextDouble() - 0.5D) * 0.025D,
                        level.random.nextDouble() * 0.025D,
                        (level.random.nextDouble() - 0.5D) * 0.025D);
        sendVelocity(level, ParticleTypes.POOF, particlePosition, dustVelocity, 160.0D);
        blockDebris(level, particlePosition, stonePellet ? 2 : 1,
                stonePellet ? 0.09D : 0.065D, 160.0D);
    }

    public void rocketExplosion(ServerLevel level, Vec3 center) {
        sendLongRange(level, ParticleTypes.FLASH, center, 1, Vec3.ZERO, 0.0D, 150.0D);
        sendLongRange(level, ParticleTypes.EXPLOSION, center, 1,
                new Vec3(0.08D, 0.06D, 0.08D), 0.0D, 150.0D);
        sendLongRange(level, ParticleTypes.FIREWORK, center, 12,
                new Vec3(0.42D, 0.32D, 0.42D), 0.14D, 150.0D);
        sendLongRange(level, ParticleTypes.FLAME, center, 18,
                new Vec3(0.38D, 0.28D, 0.38D), 0.075D, 150.0D);
        sendLongRange(level, particles.SMOKE.get(), center, 14,
                new Vec3(0.3D, 0.2D, 0.3D), 0.025D, 150.0D);
        sendLongRange(level, ParticleTypes.LAVA, center, 6,
                new Vec3(0.3D, 0.18D, 0.3D), 0.1D, 150.0D);
        radialDust(level, center, 10, 0.75D, 150.0D);
        blockDebris(level, center, 18, 0.42D, 150.0D);
    }

    /** Pot shatter particles, visible at the same range as the projectile. */
    public void potShatter(ServerLevel level, Vec3 center, BlockState pot) {
        sendLongRange(level, new BlockParticleOption(ParticleTypes.BLOCK, pot), center, 40,
                new Vec3(0.25D, 0.2D, 0.25D), 0.2D, 220.0D);
        sendLongRange(level, ParticleTypes.POOF, center, 6, new Vec3(0.2D, 0.1D, 0.2D), 0.02D, 220.0D);
    }

    public void incendiaryImpact(ServerLevel level, Vec3 center, int fireRadius) {
        double scale = clamp(fireRadius / 5.0D, 0.8D, 1.6D);
        sendLongRange(level, ParticleTypes.FLASH, center, 1, Vec3.ZERO, 0.0D, 220.0D);
        sendLongRange(level, ParticleTypes.EXPLOSION, center, 2,
                new Vec3(0.1D, 0.07D, 0.1D), 0.0D, 220.0D);
        sendLongRange(level, ParticleTypes.LAVA, center.add(0.0D, 0.15D, 0.0D),
                36, new Vec3(0.8D * scale, 0.45D * scale, 0.8D * scale), 0.2D, 220.0D);
        sendLongRange(level, ParticleTypes.FLAME, center.add(0.0D, 0.18D, 0.0D),
                96, new Vec3(1.15D * scale, 0.68D * scale, 1.15D * scale), 0.15D, 220.0D);
        sendLongRange(level, ParticleTypes.SMALL_FLAME, center.add(0.0D, 0.38D, 0.0D),
                48, new Vec3(0.62D * scale, 0.42D * scale, 0.62D * scale), 0.22D, 220.0D);
        sendLongRange(level, particles.HEAVY_SMOKE.get(),
                center.add(0.0D, 0.2D, 0.0D),
                32, new Vec3(0.75D * scale, 0.46D * scale, 0.75D * scale), 0.045D, 220.0D);
        radialDust(level, center, 16, scale, 220.0D);
        blockDebris(level, center, 40, 0.75D * scale, 220.0D);
    }

    private void pressureCone(ServerLevel level, Vec3 origin, Vec3 forward,
                                     Basis basis, MuzzleProfile profile) {
        for (int i = 0; i < profile.puffCount; i++) {
            double progress = (i + level.random.nextDouble()) / profile.puffCount;
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            Vec3 radialDirection = basis.right.scale(Math.cos(angle))
                    .add(basis.up.scale(Math.sin(angle)));
            double distance = profile.scale * (0.025D + progress * 0.11D);
            double radius = profile.scale * (0.006D + progress * 0.035D)
                    * Math.sqrt(level.random.nextDouble());
            Vec3 position = origin.add(forward.scale(distance)).add(radialDirection.scale(radius));
            double forwardSpeed = profile.scale > 2.5D
                    ? profile.scale * (0.035D + level.random.nextDouble() * 0.03D)
                    : (0.13D + level.random.nextDouble() * 0.1D) * (0.9D + profile.scale * 0.14D);
            Vec3 velocity = forward.scale(forwardSpeed)
                    .add(radialDirection.scale(profile.scale
                            * (0.004D + level.random.nextDouble() * 0.012D)));
            sendVelocity(level, ParticleTypes.POOF, position, velocity, profile.range);
        }
    }

    private void radialDust(ServerLevel level, Vec3 center, int count, double scale, double range) {
        int clampedCount = Math.min(64, Math.max(1, count));
        for (int i = 0; i < clampedCount; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = scale * (0.015D + level.random.nextDouble() * 0.06D);
            double horizontalSpeed = scale * (0.025D + level.random.nextDouble() * 0.09D);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Vec3 position = center.add(cos * distance,
                    0.015D + level.random.nextDouble() * 0.04D * scale,
                    sin * distance);
            Vec3 velocity = new Vec3(cos * horizontalSpeed,
                    0.012D + level.random.nextDouble() * 0.055D * scale,
                    sin * horizontalSpeed);
            sendVelocity(level, ParticleTypes.POOF, position, velocity, range);
        }
    }

    private void blockDebris(ServerLevel level, Vec3 position, int count, double spread, double range) {
        BlockState state = findDebrisState(level, position);
        if (state == null) {
            return;
        }
        sendLongRange(level, new BlockParticleOption(ParticleTypes.BLOCK, state), position,
                Math.min(96, Math.max(1, count)),
                new Vec3(spread, Math.max(0.15D, spread * 0.55D), spread),
                clamp(0.08D + spread * 0.12D, 0.08D, 0.22D), range);
    }

    private static BlockState findDebrisState(ServerLevel level, Vec3 position) {
        BlockPos origin = BlockPos.containing(position);
        BlockState originState = level.getBlockState(origin);
        if (isVisibleMaterial(originState)) {
            return originState;
        }
        for (Direction direction : Direction.values()) {
            BlockState state = level.getBlockState(origin.relative(direction));
            if (isVisibleMaterial(state)) {
                return state;
            }
        }
        return null;
    }

    private static boolean isVisibleMaterial(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    private static Basis basis(Vec3 direction) {
        Vec3 referenceUp = Math.abs(direction.y) > 0.95D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = direction.cross(referenceUp).normalize();
        return new Basis(right, right.cross(direction).normalize());
    }

    private static Vec3 safeDirection(Vec3 direction) {
        return direction.lengthSqr() > 1.0E-8D ? direction.normalize() : DEFAULT_DIRECTION;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static <T extends ParticleOptions> void sendVelocity(ServerLevel level, T particle,
                                                                  Vec3 position, Vec3 velocity, double range) {
        sendLongRange(level, particle, position, 0, velocity, 1.0D, range);
    }

    private static <T extends ParticleOptions> void sendLongRange(ServerLevel level, T particle,
                                                                   Vec3 position, int count, Vec3 spread,
                                                                   double speed, double range) {
        double rangeSqr = range * range;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(position.x, position.y, position.z) <= rangeSqr) {
                level.sendParticles(player, particle, true,
                        position.x, position.y, position.z,
                        count, spread.x, spread.y, spread.z, speed);
            }
        }
    }

    private record Basis(Vec3 right, Vec3 up) {
    }
}
