package me.mss1r.axiomata.collision.system;

import java.util.ArrayList;
import java.util.List;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.StructureTransform;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class StructureMotionSystem {
    private static final double CARRY_SUPPORT_REACH = 0.03D;
    private static final double BOUNDS_MARGIN = 0.25D;
    private static final double MOTION_EPSILON = 1.0E-10D;
    private static final double ASCENDING_EPSILON = 1.0E-4D;

    private StructureMotionSystem() {
    }

    public static void tickStructure(CollidableStructure structure) {
        StructureCollisionSystem.register(structure);
        if (!(structure instanceof Entity structureEntity) || structureEntity.isRemoved()) {
            return;
        }

        List<CollisionGroup> currentGroups = structure.solidCollisionGroups();
        List<CollisionGroup> previousGroups = structure.previousSolidCollisionGroups();
        if (currentGroups.isEmpty()) {
            return;
        }

        StructureTransform currentTransform = structure.collisionTransform();
        StructureTransform previousTransform = structure.previousCollisionTransform();
        List<CollisionGroup> locallyMovedGroups = locallyMovedGroups(currentGroups, previousGroups);
        boolean rootRotated = transformRotated(currentTransform, previousTransform);
        List<CollisionGroup> movedGroups = movedGroups(
                currentTransform, previousTransform, currentGroups, locallyMovedGroups);
        if (movedGroups.isEmpty()) {
            return;
        }
        List<CollisionGroup> previousMovedGroups = matchingPreviousGroups(movedGroups, previousGroups);
        AABB currentBounds = StructureCollisionResolver.worldBounds(currentTransform, movedGroups);
        AABB previousBounds = StructureCollisionResolver.worldBounds(previousTransform, previousMovedGroups);
        AABB searchBounds = combine(currentBounds, previousBounds);
        if (searchBounds == null) {
            return;
        }

        structureEntity.level().getEntities(structureEntity, searchBounds.inflate(BOUNDS_MARGIN),
                entity -> canCollideWithStructure(structureEntity, entity)
                        && ownsDynamicStructureMovement(entity)).forEach(entity -> {
            boolean carriedWithoutClipping = false;
            // Model slopes may lose vanilla onGround for a tick. Probe real world support instead;
            // positive vertical velocity still detaches a jump.
            boolean canBeCarried = entity.getDeltaMovement().y <= ASCENDING_EPSILON
                    && !isSupportedByWorld(entity);
            CollisionGroup previousSupport = canBeCarried
                    ? StructureCollisionResolver.supportingGroup(
                            previousTransform, previousGroups, entity.getBoundingBox(), CARRY_SUPPORT_REACH)
                    : null;
            if (previousSupport != null) {
                StructureClimbingSystem.clearClimbAttachment(entity, structure);
                CollisionGroup currentGroup = groupNamed(currentGroups, previousSupport.name());
                if (currentGroup != null) {
                    Vec3 carry = carryPoint(previousTransform, previousSupport,
                            currentTransform, currentGroup, entity.position());
                    carriedWithoutClipping = movePreserved(entity, carry);
                }
            } else if (StructureClimbingSystem.intersectsAnyClimbable(
                    structure, previousTransform, entity.getBoundingBox(),
                    StructureClimbingSystem.climbableReach())
                    || StructureClimbingSystem.isNearAttachedClimbable(
                    entity, structure, previousTransform, entity.getBoundingBox())) {
                carriedWithoutClipping = movePreserved(entity,
                        currentTransform.carryFrom(previousTransform, entity.position()));
                StructureClimbingSystem.rememberClimbAttachment(entity, structure);
            }

            // Root translation preserves the relative position we just carried. Local animation
            // and root rotation can still move individual groups into the entity.
            List<CollisionGroup> overlapGroups = carriedWithoutClipping && !rootRotated
                    ? locallyMovedGroups
                    : movedGroups;
            if (overlapGroups.isEmpty()) {
                return;
            }
            StructureCollisionResolver.Response overlap = StructureCollisionResolver.resolve(
                    currentTransform, overlapGroups, entity.getBoundingBox(), Vec3.ZERO, 0.0D);
            if (!overlap.pushOut().equals(Vec3.ZERO)) {
                Vec3 push = moveDirectly(entity, overlap.pushOut());
                removeVelocityIntoPush(entity, push);
            }
        });
    }

    private static List<CollisionGroup> movedGroups(StructureTransform currentTransform,
                                                    StructureTransform previousTransform,
                                                    List<CollisionGroup> currentGroups,
                                                    List<CollisionGroup> locallyMovedGroups) {
        return transformMoved(currentTransform, previousTransform) ? currentGroups : locallyMovedGroups;
    }

    private static List<CollisionGroup> locallyMovedGroups(List<CollisionGroup> currentGroups,
                                                           List<CollisionGroup> previousGroups) {
        List<CollisionGroup> moved = new ArrayList<>();
        for (CollisionGroup current : currentGroups) {
            CollisionGroup previous = groupNamed(previousGroups, current.name());
            if (previous == null
                    || !current.shape().equals(previous.shape())
                    || !current.pose().equals(previous.pose())) {
                moved.add(current);
            }
        }
        return moved;
    }

    private static boolean transformMoved(StructureTransform currentTransform,
                                          StructureTransform previousTransform) {
        return Math.abs(currentTransform.x() - previousTransform.x()) > MOTION_EPSILON
                || Math.abs(currentTransform.y() - previousTransform.y()) > MOTION_EPSILON
                || Math.abs(currentTransform.z() - previousTransform.z()) > MOTION_EPSILON
                || Math.abs(Mth.wrapDegrees(
                currentTransform.yawDegrees() - previousTransform.yawDegrees())) > ASCENDING_EPSILON;
    }

    private static boolean transformRotated(StructureTransform currentTransform,
                                            StructureTransform previousTransform) {
        return Math.abs(Mth.wrapDegrees(
                currentTransform.yawDegrees() - previousTransform.yawDegrees())) > ASCENDING_EPSILON;
    }

    private static List<CollisionGroup> matchingPreviousGroups(List<CollisionGroup> movedGroups,
                                                                List<CollisionGroup> previousGroups) {
        List<CollisionGroup> matching = new ArrayList<>();
        for (CollisionGroup moved : movedGroups) {
            CollisionGroup previous = groupNamed(previousGroups, moved.name());
            if (previous != null) {
                matching.add(previous);
            }
        }
        return matching;
    }

    private static boolean canCollideWithStructure(Entity structure, Entity entity) {
        return StructureCollisionSystem.canCollide(entity)
                && !entity.isPassenger()
                && !entity.isPassengerOfSameVehicle(structure);
    }
    private static boolean ownsDynamicStructureMovement(Entity entity) {
        // The local client predicts its own player. Everything else remains server-owned.
        if (entity.level().isClientSide) {
            return entity instanceof Player player && player.isLocalPlayer();
        }
        return !(entity instanceof Player);
    }

    private static AABB combine(AABB first, AABB second) {
        if (first == null) {
            return second;
        }
        return second == null ? first : first.minmax(second);
    }

    private static CollisionGroup groupNamed(List<CollisionGroup> groups, String name) {
        for (CollisionGroup group : groups) {
            if (group.name().equals(name)) {
                return group;
            }
        }
        return null;
    }

    private static Vec3 carryPoint(StructureTransform previousTransform, CollisionGroup previousGroup,
                                   StructureTransform currentTransform, CollisionGroup currentGroup,
                                   Vec3 worldPoint) {
        Vec3 groupPoint = previousGroup.toGroup(previousTransform.toLocal(worldPoint));
        return currentTransform.toWorld(currentGroup.fromGroup(groupPoint)).subtract(worldPoint);
    }

    private static boolean movePreserved(Entity entity, Vec3 requested) {
        Vec3 allowed = moveDirectly(entity, requested);
        return allowed.distanceToSqr(requested) <= MOTION_EPSILON;
    }

    private static Vec3 moveDirectly(Entity entity, Vec3 requested) {
        if (requested.lengthSqr() <= MOTION_EPSILON) {
            return Vec3.ZERO;
        }
        Vec3 allowed = StructureCollisionSystem.collideWithWorld(entity, requested);
        entity.setPos(entity.getX() + allowed.x, entity.getY() + allowed.y, entity.getZ() + allowed.z);
        entity.hurtMarked = true;
        return allowed;
    }

    private static boolean isSupportedByWorld(Entity entity) {
        Vec3 probe = new Vec3(0.0D, -CARRY_SUPPORT_REACH, 0.0D);
        return StructureCollisionSystem.collideWithWorld(entity, probe).y
                > probe.y + MOTION_EPSILON;
    }

    private static void removeVelocityIntoPush(Entity entity, Vec3 push) {
        if (push.lengthSqr() <= MOTION_EPSILON) {
            return;
        }
        Vec3 normal = push.normalize();
        Vec3 velocity = entity.getDeltaMovement();
        double inward = velocity.dot(normal);
        if (inward < 0.0D) {
            entity.setDeltaMovement(velocity.subtract(normal.scale(inward)));
        }
    }
}
