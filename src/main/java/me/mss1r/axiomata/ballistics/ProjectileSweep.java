package me.mss1r.axiomata.ballistics;

import me.mss1r.axiomata.ballistics.ProjectilePassThroughControl;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.system.StructureCollisionSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class ProjectileSweep {
    private static final double SAMPLE_DISTANCE = 0.18D;
    private static final double MIN_SEGMENT_LENGTH_SQR = 1.0E-5D;

    private ProjectileSweep() {
    }

    @FunctionalInterface
    public interface BlockIgnorer {
        boolean shouldIgnore(Level world, BlockPos pos, BlockState state);
    }

    @FunctionalInterface
    public interface BlockImpactConsumer {
        void onHit(BlockHitResult hit);
    }

    public static boolean hitFirstBlockingBlock(ServerLevel serverLevel, Vec3 previousPos, Vec3 currentPos,
                                                Vec3 previousMovement, BlockIgnorer blockIgnorer,
                                                BlockImpactConsumer impactConsumer) {
        Vec3 segment = currentPos.subtract(previousPos);
        if (segment.lengthSqr() < MIN_SEGMENT_LENGTH_SQR) {
            segment = previousMovement;
        }
        if (segment.lengthSqr() < MIN_SEGMENT_LENGTH_SQR) {
            return false;
        }

        Vec3 direction = segment.normalize();
        int steps = Math.max(1, (int) Math.ceil(segment.length() / SAMPLE_DISTANCE));
        BlockPos previousBlock = null;
        for (int step = 1; step <= steps; step++) {
            Vec3 sample = previousPos.add(segment.scale(step / (double) steps));
            BlockPos pos = BlockPos.containing(sample);
            if (pos.equals(previousBlock)) {
                continue;
            }
            previousBlock = pos;

            BlockState state = serverLevel.getBlockState(pos);
            if (blockIgnorer.shouldIgnore(serverLevel, pos, state)) {
                continue;
            }

            Direction face = Direction.getNearest(-direction.x, -direction.y, -direction.z);
            impactConsumer.onHit(new BlockHitResult(sample, face, pos, false));
            return true;
        }
        return false;
    }

    public static EntityHitResult findFirstEntityHit(ServerLevel serverLevel, Entity projectile,
                                                     Vec3 previousPos, Vec3 currentPos,
                                                     Vec3 previousMovement, Predicate<Entity> targetPredicate,
                                                     double hitboxPadding) {
        Vec3 segment = currentPos.subtract(previousPos);
        if (segment.lengthSqr() < MIN_SEGMENT_LENGTH_SQR) {
            segment = previousMovement;
        }
        if (segment.lengthSqr() < MIN_SEGMENT_LENGTH_SQR) {
            return null;
        }

        Vec3 end = previousPos.add(segment);
        AABB searchBox = new AABB(previousPos, end)
                .inflate(Math.max(0.25D, hitboxPadding))
                .expandTowards(segment);
        Entity closestEntity = null;
        Vec3 closestHit = null;
        double closestDistanceSqr = Double.MAX_VALUE;

        Set<Entity> candidates = Collections.newSetFromMap(new IdentityHashMap<>());
        candidates.addAll(serverLevel.getEntities(projectile, searchBox, targetPredicate));
        for (CollidableStructure structure : StructureCollisionSystem.structuresNear(projectile, searchBox)) {
            if (structure instanceof Entity entity && targetPredicate.test(entity)) {
                candidates.add(entity);
            }
        }

        for (Entity target : candidates) {
            if (!(target instanceof CollidableStructure)
                    && target instanceof ProjectilePassThroughControl passThrough
                    && passThrough.allowsProjectilePassage(previousPos, end)) {
                continue;
            }
            Optional<Vec3> hit;
            if (target instanceof CollidableStructure structure && !structure.collisionGroups().isEmpty()) {
                AABB modelBounds = StructureCollisionResolver.worldBounds(structure);
                if (modelBounds == null || !modelBounds.inflate(hitboxPadding).intersects(searchBox)) {
                    continue;
                }
                hit = StructureCollisionResolver.clipSegment(structure, previousPos, end, hitboxPadding);
            } else {
                AABB targetBox = target.getBoundingBox().inflate(hitboxPadding);
                hit = targetBox.contains(previousPos)
                        ? Optional.of(previousPos)
                        : targetBox.clip(previousPos, end);
            }
            if (hit.isEmpty()) {
                continue;
            }

            Vec3 hitLocation = hit.get();
            double distanceSqr = previousPos.distanceToSqr(hitLocation);
            if (distanceSqr >= closestDistanceSqr || isBlockedBeforeHit(serverLevel, projectile, previousPos, hitLocation, distanceSqr)) {
                continue;
            }

            closestEntity = target;
            closestHit = hitLocation;
            closestDistanceSqr = distanceSqr;
        }

        return closestEntity == null ? null : new EntityHitResult(closestEntity, closestHit);
    }

    private static boolean isBlockedBeforeHit(ServerLevel serverLevel, Entity projectile, Vec3 previousPos,
                                              Vec3 hitLocation, double entityDistanceSqr) {
        BlockHitResult blockHit = serverLevel.clip(new ClipContext(previousPos, hitLocation,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile));
        return blockHit.getType() == HitResult.Type.BLOCK
                && previousPos.distanceToSqr(blockHit.getLocation()) + 1.0E-5D < entityDistanceSqr;
    }
}
