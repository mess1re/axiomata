package me.mss1r.axiomata.ballistics.profile;

import me.mss1r.axiomata.data.profile.ProfileValidation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.mss1r.axiomata.ResourceIds;
import net.minecraft.resources.ResourceLocation;
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
 * @param hardness maximum block resistance the projectile can penetrate
 * @param casingMass mass of the payload casing available for fragmentation, in kilograms; part of the total mass
 */
public record ProjectilePhysicsProfile(
        double mass,
        double dragCoefficient,
        Optional<Double> diameter,
        double hardness,
        Optional<Motor> motor,
        ImpactMaterialProfile material,
        ResourceLocation fragmentBlock,
        double casingMass,
        Blast blast,
        Fire fire,
        DirectHit directHit
) {
    public ProjectilePhysicsProfile(double mass, double dragCoefficient, Optional<Double> diameter, double hardness,
                                    Optional<Motor> motor, ImpactMaterialProfile material, ResourceLocation fragmentBlock,
                                    double casingMass, Blast blast, Fire fire) {
        this(mass, dragCoefficient, diameter, hardness, motor, material, fragmentBlock, casingMass, blast, fire, DirectHit.DEFAULT);
    }

    /** Literal HP override for this ammunition; an absent value uses the firing engine's calibration. */
    public record DirectHit(Optional<Double> damage, double armorPiercing, ResourceLocation damageType, Optional<Double> referenceSpeed) {
        public static final DirectHit DEFAULT = new DirectHit(Optional.empty(), 0, ResourceIds.id("minecraft", "thrown"), Optional.empty());
        public static final Codec<DirectHit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("damage").forGetter(DirectHit::damage),
                Codec.DOUBLE.optionalFieldOf("armorPiercing", 0.0).forGetter(DirectHit::armorPiercing),
                ResourceLocation.CODEC.optionalFieldOf("damageType", ResourceIds.id("minecraft", "thrown")).forGetter(DirectHit::damageType),
                Codec.DOUBLE.optionalFieldOf("referenceSpeed").forGetter(DirectHit::referenceSpeed)
        ).apply(instance, DirectHit::new));
    }

    /** Air at sea level, in kilograms per cubic metre. */
    private static final double AIR_DENSITY = 1.225D;

    public static final ProjectilePhysicsProfile DEFAULT = new ProjectilePhysicsProfile(
            1.0D, 0.47D, Optional.empty(), 6.0D, Optional.empty(),
            ImpactMaterialProfile.IRON, ResourceIds.id("minecraft", "iron_block"), 0, Blast.NONE, Fire.NONE
    );

    public static final Codec<ProjectilePhysicsProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("mass", DEFAULT.mass()).forGetter(ProjectilePhysicsProfile::mass),
            Codec.DOUBLE.optionalFieldOf("dragCoefficient", DEFAULT.dragCoefficient())
                    .forGetter(ProjectilePhysicsProfile::dragCoefficient),
            Codec.DOUBLE.optionalFieldOf("diameter").forGetter(ProjectilePhysicsProfile::diameter),
            Codec.DOUBLE.optionalFieldOf("hardness", DEFAULT.hardness()).forGetter(ProjectilePhysicsProfile::hardness),
            Motor.CODEC.optionalFieldOf("motor").forGetter(ProjectilePhysicsProfile::motor),
            ImpactMaterialProfile.CODEC.optionalFieldOf("material", DEFAULT.material()).forGetter(ProjectilePhysicsProfile::material),
            ResourceLocation.CODEC.optionalFieldOf("fragmentBlock", DEFAULT.fragmentBlock()).forGetter(ProjectilePhysicsProfile::fragmentBlock),
            Codec.DOUBLE.optionalFieldOf("casingMass", 0.0D).forGetter(ProjectilePhysicsProfile::casingMass),
            Blast.CODEC.optionalFieldOf("blast", Blast.NONE).forGetter(ProjectilePhysicsProfile::blast),
            Fire.CODEC.optionalFieldOf("fire", Fire.NONE).forGetter(ProjectilePhysicsProfile::fire),
            DirectHit.CODEC.optionalFieldOf("directHit", DirectHit.DEFAULT).forGetter(ProjectilePhysicsProfile::directHit)
    ).apply(instance, ProjectilePhysicsProfile::new));

    public Optional<String> validationError() {
        if (!(mass > 0.0D) || !Double.isFinite(mass)) {
            return Optional.of("mass must be greater than zero");
        }
        if (!(casingMass >= 0 && casingMass <= mass)) return Optional.of("casingMass must be from zero to the total projectile mass");
        if (diameter.isPresent() && (!(diameter.get() > 0.0D) || !Double.isFinite(diameter.get()))) {
            return Optional.of("diameter must be greater than zero");
        }
        if (motor.isPresent() && (!(motor.get().thrust() > 0.0D && motor.get().burnTime() > 0.0D)
                || !Double.isFinite(motor.get().thrust()) || !Double.isFinite(motor.get().burnTime()))) {
            return Optional.of("motor thrust and burnTime must be greater than zero");
        }
        if (!Double.isFinite(blast.radius()) || blast.radius() < 0 || !Double.isFinite(blast.damage()) || blast.damage() < 0)
            return Optional.of("blast radius and damage must be finite and nonnegative");
        if ((blast.radius() > 0) != (blast.damage() > 0)) return Optional.of("blast radius and damage must be supplied together");
        if (fire.chance() > 1.0D) {
            return Optional.of("fire.chance must not exceed one");
        }
        if (fire.burnSeconds() < 0 || fire.burnSeconds() > Integer.MAX_VALUE / 20) return Optional.of("fire.burnSeconds is out of range");
        if (directHit.damage().isPresent() && (!(directHit.damage().get() >= 0) || !Double.isFinite(directHit.damage().get())))
            return Optional.of("directHit.damage must be finite and nonnegative");
        if (!(directHit.armorPiercing() >= 0 && directHit.armorPiercing() <= 1)) return Optional.of("directHit.armorPiercing must be from zero to one");
        if (directHit.referenceSpeed().isPresent() && (!(directHit.referenceSpeed().get() > 0) || !Double.isFinite(directHit.referenceSpeed().get())))
            return Optional.of("directHit.referenceSpeed must be finite and positive");
        var materialError = material.validationError();
        if (materialError.isPresent()) return materialError;
        return Stream.of(
                        ProfileValidation.nonNegative("dragCoefficient", dragCoefficient),
                        ProfileValidation.nonNegative("hardness", hardness),
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
     * Quadratic drag coefficient per metre: airDensity * Cd * area / (2 * mass). One block is one metre.
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

    /** Chemical energy an explosive charge adds where the projectile stops. */
    public record Blast(double energy, double radius, double damage) {
        public Blast(double energy) { this(energy, 0, 0); }
        public static final Blast NONE = new Blast(0.0D);
        public static final Codec<Blast> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("energy", 0.0).forGetter(Blast::energy),
                Codec.DOUBLE.optionalFieldOf("radius", 0.0).forGetter(Blast::radius),
                Codec.DOUBLE.optionalFieldOf("damage", 0.0).forGetter(Blast::damage)
        ).apply(instance, Blast::new));
    }

    /** Fire spread where the projectile stops: radius, and ignition chance near the centre. */
    public record Fire(double radius, double chance, int burnSeconds) {
        public Fire(double radius, double chance) { this(radius, chance, 5); }
        public static final Fire NONE = new Fire(0.0D, 0.0D, 0);
        public static final Codec<Fire> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("radius", 0.0D).forGetter(Fire::radius),
                Codec.DOUBLE.optionalFieldOf("chance", 0.0D).forGetter(Fire::chance),
                Codec.INT.optionalFieldOf("burnSeconds", 5).forGetter(Fire::burnSeconds)
        ).apply(instance, Fire::new));
    }
}
