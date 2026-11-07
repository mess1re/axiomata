package me.mss1r.axiomata.ballistics;

import me.mss1r.axiomata.ballistics.impact.Fragmentation;
import me.mss1r.axiomata.ballistics.impact.FragmentSample;
import me.mss1r.axiomata.ballistics.impact.LayeredImpact;
import me.mss1r.axiomata.ballistics.profile.ImpactMaterialProfile;
import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.system.StructureCollisionSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

/** Bounded statistical samples of individual fragments. Weight changes expected hit count, never fragment geometry. */
public final class FragmentFlight {
    private static final int TRAJECTORIES_PER_CLOUD = 32;
    private static final int TRACES_PER_TICK = 2048;
    private static final int MAX_FLIGHT_TICKS = 100;
    private static final double MIN_ENERGY_J = 0.25;
    private static final Map<ServerLevel, List<Cloud>> CLOUDS = new WeakHashMap<>();
    private FragmentFlight() {}

    public static void clear() { CLOUDS.clear(); }

    public static int activeTrajectories(ServerLevel level) {
        return CLOUDS.getOrDefault(level, List.of()).stream().mapToInt(cloud -> cloud.packets.size()).sum();
    }

    public static void emit(ServerLevel level, ImpactResolver impacts, ImpactSource source, Vec3 at, Vec3 axis,
                            Fragmentation.Cloud fragments, ImpactMaterialProfile material, ResourceLocation visualBlock,
                            double hardness, @Nullable Player breaker) {
        if (fragments.count() == 0 || fragments.kineticEnergyJ() < MIN_ENERGY_J) return;
        int count = (int) Math.min(TRAJECTORIES_PER_CLOUD, fragments.count());
        var packets = new ArrayList<Packet>(count);
        Vec3 forward = axis.lengthSqr() > 1e-12 ? axis.normalize() : Vec3.ZERO;
        Vec3 reference = Math.abs(forward.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = forward.lengthSqr() > 0 ? forward.cross(reference).normalize() : new Vec3(1, 0, 0);
        Vec3 up = forward.lengthSqr() > 0 ? right.cross(forward) : new Vec3(0, 1, 0);
        long phaseSeed = Double.doubleToLongBits(at.x) ^ Long.rotateLeft(Double.doubleToLongBits(at.y), 21)
                ^ Long.rotateLeft(Double.doubleToLongBits(at.z), 42) ^ level.getGameTime();
        double phase = new java.util.SplittableRandom(phaseSeed).nextDouble() * Math.PI * 2;
        for (int i = 0; i < count; i++) {
            var sample = FragmentSample.of(fragments, count);
            // Equal-solid-angle, stratified directions. An impact emits into its outward hemisphere; a casing bursts all around.
            double axial = forward.lengthSqr() > 0 ? (i + 0.5) / count : 1 - 2 * (i + 0.5) / count;
            double radial = Math.sqrt(Math.max(0, 1 - axial * axial));
            double angle = phase + i * Math.PI * (3 - Math.sqrt(5));
            Vec3 direction = forward.lengthSqr() > 0
                    ? forward.scale(axial).add(right.scale(radial * Math.cos(angle))).add(up.scale(radial * Math.sin(angle)))
                    : new Vec3(radial * Math.cos(angle), axial, radial * Math.sin(angle));
            double mass = sample.massKg();
            double diameter = sample.diameterM();
            var profile = new ProjectilePhysicsProfile(mass, 0.8, Optional.of(diameter), hardness, Optional.empty(),
                    material, visualBlock, 0, ProjectilePhysicsProfile.Blast.NONE, ProjectilePhysicsProfile.Fire.NONE);
            packets.add(new Packet(at, direction.scale(fragments.speedMps() / 20), profile, sample.weight()));
        }
        CLOUDS.computeIfAbsent(level, ignored -> new ArrayList<>()).add(new Cloud(impacts, source, packets, breaker));
        var state = BuiltInRegistries.BLOCK.get(visualBlock).defaultBlockState();
        var particle = new BlockParticleOption(BallisticsCommon.PARTICLES.FRAGMENT.get(), state);
        for (Packet packet : packets) {
            // A zero count sends one particle with this exact initial velocity, without Gaussian scatter.
            level.sendParticles(particle, at.x, at.y, at.z, 0,
                    packet.velocity.x, packet.velocity.y, packet.velocity.z, 1);
        }
    }

    public static void tick(ServerLevel level) {
        List<Cloud> clouds = CLOUDS.remove(level);
        if (clouds == null) return;
        int budget = TRACES_PER_TICK;
        var surviving = new ArrayList<Cloud>();
        var deferred = new ArrayList<Cloud>();
        for (Cloud cloud : clouds) {
            if (budget == 0) { deferred.add(cloud); continue; }
            var work = new IdentityHashMap<Entity, HitWork>();
            int remaining = cloud.packets.size();
            while (budget > 0 && remaining-- > 0 && !cloud.packets.isEmpty()) {
                int index = cloud.cursor % cloud.packets.size();
                Packet packet = cloud.packets.get(index);
                budget--;
                if (step(level, cloud, packet, work)) cloud.cursor = index + 1;
                else { cloud.packets.remove(index); cloud.cursor = index; }
            }
            for (var hit : work.entrySet()) {
                LivingEntity target = ProjectileCombat.livingTarget(hit.getKey());
                if (target == null) continue;
                float damage = (float) hit.getValue().damageHp();
                if (damage > 0) cloud.source.damage().apply(hit.getKey(), damage);
                if (target.isAlive()) {
                    target.setDeltaMovement(target.getDeltaMovement().add(hit.getValue().velocityChangeMps().scale(1.0 / 20)));
                    target.hurtMarked = true;
                }
            }
            if (!cloud.packets.isEmpty()) surviving.add(cloud);
        }
        // New clouds generated by these impacts are processed on the next tick, never recursively in this loop.
        var next = CLOUDS.computeIfAbsent(level, ignored -> new ArrayList<>());
        next.addAll(deferred);
        next.addAll(surviving);
        if (CLOUDS.get(level).isEmpty()) CLOUDS.remove(level);
    }

    private static boolean step(ServerLevel level, Cloud cloud, Packet packet, Map<Entity, HitWork> work) {
        if (++packet.age > MAX_FLIGHT_TICKS || packet.energy() <= 1e-9) return false;
        Vec3 end = packet.position.add(packet.velocity);
        if (!level.hasChunkAt(BlockPos.containing(packet.position)) || !level.hasChunkAt(BlockPos.containing(end))) return false;
        var block = BlockClip.colliders(level, packet.position, end);
        Vec3 limit = block.getType() == HitResult.Type.BLOCK ? block.getLocation() : end;
        double nearest = packet.position.distanceToSqr(limit);
        LivingEntity hit = null;
        Entity hitGeometry = null;
        Vec3 hitAt = null;
        AABB bounds = new AABB(packet.position, limit).inflate(0.05);
        Set<Entity> candidates = new HashSet<>(level.getEntities((Entity) null, bounds, target -> {
            var living = ProjectileCombat.livingTarget(target);
            return living != null && living.isAlive() && cloud.source.mayHit().test(target);
        }));
        Entity context = cloud.source.direct() != null ? cloud.source.direct() : cloud.source.owner();
        if (context != null) for (var structure : StructureCollisionSystem.structuresNear(context, bounds)) {
            if (structure instanceof LivingEntity entity && entity.isAlive() && cloud.source.mayHit().test(entity)) candidates.add(entity);
        }
        for (Entity candidate : candidates) {
            LivingEntity logical = ProjectileCombat.livingTarget(candidate);
            if (logical == null || packet.hit.contains(logical.getId())) continue;
            Optional<Vec3> contact = candidate instanceof CollidableStructure structure
                    ? StructureCollisionResolver.clipSegment(structure, packet.position, limit)
                    : candidate.getBoundingBox().contains(packet.position) ? Optional.of(packet.position)
                    : candidate.getBoundingBox().clip(packet.position, limit);
            if (contact.isPresent() && packet.position.distanceToSqr(contact.get()) <= nearest) {
                nearest = packet.position.distanceToSqr(contact.get()); hit = logical; hitGeometry = candidate; hitAt = contact.get();
            }
        }
        if (hit != null) {
            var result = ProjectilePhysics.resolve(LayeredImpact.Shot.circular(packet.profile.mass(), packet.profile.diameter().orElseThrow()),
                    packet.velocity.scale(20), hitAt, hit, hitGeometry);
            work.merge(hit, new HitWork(ProjectilePhysics.secondaryDamageForWork(hit, hitAt, result.workJ()) * packet.weight, result.targetVelocityMps().subtract(hit.getDeltaMovement().scale(20)).scale(packet.weight)),
                    (first, next) -> new HitWork(first.damageHp() + next.damageHp(),
                            first.velocityChangeMps().add(next.velocityChangeMps())));
            packet.hit.add(hit.getId());
            if (!result.passedThrough()) return false;
            packet.position = result.exitPosition();
            packet.velocity = packet.velocity.normalize().scale(result.exitSpeedMps() / 20);
        } else if (block.getType() == HitResult.Type.BLOCK) {
            // Fine fragment passage uses its own contact work; it never invokes the artillery's whole-voxel bore rule.
            var drive = cloud.impacts.fragmentPassage(level, packet.profile, block, end, packet.velocity.scale(20),
                    cloud.source.mayDamageBlocks(), cloud.breaker, packet.weight);
            if (!drive.passedThrough()) return false;
            packet.position = drive.position();
            packet.velocity = packet.velocity.normalize().scale(drive.speed() / 20);
        } else packet.position = end;
        var flight = Ballistics.Flight.ballistic(packet.profile.airDrag(packet.profile.diameter().orElseThrow()));
        packet.velocity = flight.afterMove(packet.velocity);
        return true;
    }

    private record HitWork(double damageHp, Vec3 velocityChangeMps) {}

    private static final class Packet {
        Vec3 position, velocity;
        final ProjectilePhysicsProfile profile;
        final double weight;
        final Set<Integer> hit = new HashSet<>();
        int age;
        Packet(Vec3 position, Vec3 velocity, ProjectilePhysicsProfile profile, double weight) {
            this.position = position; this.velocity = velocity; this.profile = profile; this.weight = weight;
        }
        double energy() { return profile.kineticEnergy(velocity.length() * 20); }
    }

    private static final class Cloud {
        final ImpactResolver impacts;
        final ImpactSource source;
        final List<Packet> packets;
        @Nullable final Player breaker;
        int cursor;
        Cloud(ImpactResolver impacts, ImpactSource source, List<Packet> packets, @Nullable Player breaker) {
            this.impacts = impacts; this.source = source; this.packets = packets; this.breaker = breaker;
        }
    }
}
