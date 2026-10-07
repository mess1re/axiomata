package me.mss1r.axiomata.ballistics.profile;

import me.mss1r.axiomata.data.profile.ProfileValidation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Flight and impact settings: mass in kilograms, diameter in metres, blast energy in joules.
 *
 * @param dragCoefficient dimensionless air resistance, applied over the projectile's cross-section
 * @param diameter the width it flies and strikes with; the entity's own width when absent
 * @param motor the rocket motor that drives it, if it has one
 * @param hardness maximum block resistance the projectile can penetrate; also controls shattering
 */
public record ProjectilePhysicsProfile(
        double mass,
        double dragCoefficient,
        Optional<Double> diameter,
        double hardness,
        Optional<Motor> motor,
        EntityHit entity,
        Shock shock,
        Blast blast,
        Fire fire
) {
    /** Air at sea level, in kg/m³. */
    private static final double AIR_DENSITY = 1.225D;

    public static final ProjectilePhysicsProfile DEFAULT = new ProjectilePhysicsProfile(
            1.0D, 0.47D, Optional.empty(), 6.0D, Optional.empty(),
            EntityHit.DEFAULT, Shock.NONE, Blast.NONE, Fire.NONE
    );

    public static final Codec<ProjectilePhysicsProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("mass", DEFAULT.mass()).forGetter(ProjectilePhysicsProfile::mass),
            Codec.DOUBLE.optionalFieldOf("dragCoefficient", DEFAULT.dragCoefficient())
                    .forGetter(ProjectilePhysicsProfile::dragCoefficient),
            Codec.DOUBLE.optionalFieldOf("diameter").forGetter(ProjectilePhysicsProfile::diameter),
            Codec.DOUBLE.optionalFieldOf("hardness", DEFAULT.hardness()).forGetter(ProjectilePhysicsProfile::hardness),
            Motor.CODEC.optionalFieldOf("motor").forGetter(ProjectilePhysicsProfile::motor),
            EntityHit.CODEC.optionalFieldOf("entity", EntityHit.DEFAULT).forGetter(ProjectilePhysicsProfile::entity),
            Shock.CODEC.optionalFieldOf("shock", Shock.NONE).forGetter(ProjectilePhysicsProfile::shock),
            Blast.CODEC.optionalFieldOf("blast", Blast.NONE).forGetter(ProjectilePhysicsProfile::blast),
            Fire.CODEC.optionalFieldOf("fire", Fire.NONE).forGetter(ProjectilePhysicsProfile::fire)
    ).apply(instance, ProjectilePhysicsProfile::new));

    public Optional<String> validationError() {
        if (!(mass > 0.0D) || !Double.isFinite(mass)) {
            return Optional.of("mass must be greater than zero");
        }
        if (diameter.isPresent() && (!(diameter.get() > 0.0D) || !Double.isFinite(diameter.get()))) {
            return Optional.of("diameter must be greater than zero");
        }
        if (motor.isPresent() && (!(motor.get().thrust() > 0.0D && motor.get().burnTime() > 0.0D)
                || !Double.isFinite(motor.get().thrust()) || !Double.isFinite(motor.get().burnTime()))) {
            return Optional.of("motor thrust and burnTime must be greater than zero");
        }
        if (entity.armorPiercing() > 1.0D || fire.chance() > 1.0D) {
            return Optional.of("entity.armorPiercing and fire.chance must not exceed one");
        }
        return Stream.of(
                        ProfileValidation.nonNegative("dragCoefficient", dragCoefficient),
                        ProfileValidation.nonNegative("hardness", hardness),
                        ProfileValidation.nonNegative("entity.damage", entity.damage()),
                        ProfileValidation.nonNegative("entity.armorPiercing", entity.armorPiercing()),
                        ProfileValidation.nonNegative("entity.structure", entity.structure()),
                        ProfileValidation.nonNegative("shock.radius", shock.radius()),
                        ProfileValidation.nonNegative("shock.damage", shock.damage()),
                        ProfileValidation.nonNegative("blast.energy", blast.energy()),
                        ProfileValidation.nonNegative("fire.radius", fire.radius()),
                        ProfileValidation.nonNegative("fire.chance", fire.chance()))
                .flatMap(Optional::stream)
                .findFirst();
    }

    /** Energy in joules at {@code speed} metres per second. */
    public double kineticEnergy(double speed) {
        return 0.5D * mass * speed * speed;
    }

    public double diameterOf(Entity projectile) {
        return diameter.orElseGet(() -> (double) projectile.getBbWidth());
    }

    public double diameterOf(EntityType<?> projectile) {
        return diameter.orElseGet(() -> (double) projectile.getWidth());
    }

    /**
     * Speed lost per block travelled at 1 block/tick: quadratic drag, {@code ρ·Cd·A / 2m} per metre (1 block = 1 m).
     */
    public double airDrag(double diameter) {
        double area = Math.PI * diameter * diameter / 4.0D;
        return AIR_DENSITY * dragCoefficient * area / (2.0D * mass);
    }

    /** A rocket motor: its thrust in newtons, and how long it burns in seconds. */
    public record Motor(double thrust, double burnTime) {
        public static final Codec<Motor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf("thrust").forGetter(Motor::thrust),
                Codec.DOUBLE.fieldOf("burnTime").forGetter(Motor::burnTime)
        ).apply(instance, Motor::new));
    }

    /**
     * Direct-hit damage multiplier on the engine's base damage, armor penetration, and the multiplier applied when the
     * target is a structure.
     */
    public record EntityHit(double damage, double armorPiercing, double structure) {
        public static final EntityHit DEFAULT = new EntityHit(1.0D, 0.0D, 0.7D);
        public static final Codec<EntityHit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("damage", DEFAULT.damage()).forGetter(EntityHit::damage),
                Codec.DOUBLE.optionalFieldOf("armorPiercing", DEFAULT.armorPiercing())
                        .forGetter(EntityHit::armorPiercing),
                Codec.DOUBLE.optionalFieldOf("structure", DEFAULT.structure()).forGetter(EntityHit::structure)
        ).apply(instance, EntityHit::new));
    }

    /** Damage to creatures around the impact, as a share of the engine's base damage. */
    public record Shock(double radius, double damage) {
        public static final Shock NONE = new Shock(0.0D, 0.0D);
        public static final Codec<Shock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("radius", 0.0D).forGetter(Shock::radius),
                Codec.DOUBLE.optionalFieldOf("damage", 0.0D).forGetter(Shock::damage)
        ).apply(instance, Shock::new));

        public double radiusAt(double speed) {
            return radius * Mth.clamp(0.85D + Math.log1p(Math.max(0.0D, speed)) * 0.18D, 0.75D, 1.45D);
        }

        public double damageAt(double speed) {
            return damage * Mth.clamp(0.85D + Math.log1p(Math.max(0.0D, speed)) * 0.2D, 0.75D, 1.55D);
        }
    }

    /** Chemical energy an explosive charge adds where the projectile stops. */
    public record Blast(double energy) {
        public static final Blast NONE = new Blast(0.0D);
        public static final Codec<Blast> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("energy", 0.0D).forGetter(Blast::energy)
        ).apply(instance, Blast::new));
    }

    /** Fire spread where the projectile stops: radius, and ignition chance near the centre. */
    public record Fire(double radius, double chance) {
        public static final Fire NONE = new Fire(0.0D, 0.0D);
        public static final Codec<Fire> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("radius", 0.0D).forGetter(Fire::radius),
                Codec.DOUBLE.optionalFieldOf("chance", 0.0D).forGetter(Fire::chance)
        ).apply(instance, Fire::new));
    }
}
