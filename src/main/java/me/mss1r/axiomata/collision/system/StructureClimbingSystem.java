package me.mss1r.axiomata.collision.system;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import me.mss1r.axiomata.collision.ClimbableGroup;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPart;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.StructureTransform;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class StructureClimbingSystem {
    private static final double CLIMBABLE_REACH = 0.12D;
    private static final double ATTACHED_CLIMBABLE_REACH = 0.4D;
    private static final double CLIMB_INPUT_EPSILON = 1.0E-4D;
    private static final double CLIMB_SPEED = 0.2D;
    private static final double CLIMB_DESCENT_SPEED = 0.15D;
    private static final double CLIMB_CENTERING_SPEED = 0.1D;
    // The pose should end when the hands leave the rungs, before the feet reach the landing.
    private static final double HAND_HEIGHT_FRACTION = 0.72D;
    private static final long CLIMB_CARRY_GRACE_TICKS = 3L;
    private static final float CLIMB_POSE_FADE_IN_TICKS = 4.0F;
    private static final float CLIMB_POSE_HOLD_TICKS = 3.0F;
    private static final float CLIMB_POSE_FADE_OUT_TICKS = 5.0F;
    private static final Map<Entity, ClimbAttachment> CLIMB_ATTACHMENTS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Entity, ClimbPoseAttachment> CLIMB_POSE_ATTACHMENTS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private StructureClimbingSystem() {
    }

    static double climbableReach() {
        return CLIMBABLE_REACH;
    }

    public static boolean isOnClimbable(Entity entity) {
        if (!StructureCollisionSystem.canCollide(entity)) {
            return false;
        }
        AABB bounds = entity.getBoundingBox().inflate(CLIMBABLE_REACH);
        for (CollidableStructure structure : StructureCollisionSystem.structuresNear(entity, bounds)) {
            for (ClimbableGroup climbable : structure.climbableGroups()) {
                if (intersectsClimbable(structure, climbable, entity.getBoundingBox())) {
                    rememberClimbAttachment(entity, structure);
                    VirtualPlatformSupport.markSupported(entity);
                    return true;
                }
            }
            if (isNearAttachedClimbable(
                    entity, structure, structure.collisionTransform(), entity.getBoundingBox())) {
                // The wider envelope only keeps an existing attachment alive through tick-order
                // jitter. It must never create a new ladder attachment by itself.
                rememberClimbAttachment(entity, structure);
                VirtualPlatformSupport.markSupported(entity);
                return true;
            }
        }
        return false;
    }

    public static Vec3 applyClimbableVelocity(LivingEntity entity, Vec3 movement) {
        return applyClimbableVelocity(entity, movement, ClimbIntent.AUTOMATIC);
    }

    public static Vec3 applyPlayerClimbableVelocity(Player player, Vec3 movement, boolean jumping) {
        ClimbIntent intent = jumping
                ? ClimbIntent.UP
                : player.isShiftKeyDown() ? ClimbIntent.DOWN : ClimbIntent.NONE;
        return applyClimbableVelocity(player, movement, intent);
    }

    private static Vec3 applyClimbableVelocity(LivingEntity entity, Vec3 movement, ClimbIntent intent) {
        if (intent == ClimbIntent.NONE) {
            return movement;
        }

        AABB path = entity.getBoundingBox().expandTowards(movement).inflate(CLIMBABLE_REACH);
        for (CollidableStructure structure : StructureCollisionSystem.structuresNear(entity, path)) {
            for (ClimbableGroup climbable : structure.climbableGroups()) {
                if (intersectsClimbable(structure, climbable,
                        entity.getBoundingBox().expandTowards(movement))) {
                    rememberClimbAttachment(entity, structure);
                    VirtualPlatformSupport.markSupported(entity);
                    Vec3 climbing = climbMovement(structure, climbable, entity, movement, intent);
                    if (climbing != null) {
                        entity.fallDistance = 0.0F;
                        return climbing;
                    }
                }
            }
            if (hasRecentClimbAttachment(entity, structure)) {
                for (ClimbableGroup climbable : structure.climbableGroups()) {
                    if (!intersectsClimbable(structure.collisionTransform(), climbable,
                            entity.getBoundingBox().expandTowards(movement), ATTACHED_CLIMBABLE_REACH)) {
                        continue;
                    }
                    rememberClimbAttachment(entity, structure);
                    VirtualPlatformSupport.markSupported(entity);
                    Vec3 climbing = climbMovement(structure, climbable, entity, movement, intent);
                    if (climbing != null) {
                        entity.fallDistance = 0.0F;
                        return climbing;
                    }
                }
            }
        }
        return movement;
    }
    // Guided movement discards sideways input and centres the entity on the ladder. The collision
    // system may ignore the climbed structure only for this exact tick, not for the grace period.
    private static final Map<Entity, GuidedClimb> GUIDED_CLIMBS = new WeakHashMap<>();
    public static boolean isGuidedClimbing(Entity entity, CollidableStructure structure) {
        GuidedClimb guided = GUIDED_CLIMBS.get(entity);
        return guided != null
                && guided.structure().get() == structure
                && guided.tick() == entity.level().getGameTime();
    }

    private record GuidedClimb(WeakReference<CollidableStructure> structure, long tick) {
    }

    private static Vec3 climbMovement(CollidableStructure structure, ClimbableGroup climbable,
                                      LivingEntity entity, Vec3 movement, ClimbIntent intent) {
        StructureTransform transform = structure.collisionTransform();
        CollisionGroup collision = climbable.collision();
        Vec3 structurePosition = transform.toLocal(entity.position());
        if (intent != ClimbIntent.DOWN && structurePosition.y >= climbable.releaseY()) {
            return null;
        }
        Vec3 groupMovement = collision.toGroupDirection(transform.directionToLocal(movement));
        double approach = climbable.approachAxis() == Direction.Axis.X
                ? groupMovement.x
                : groupMovement.z;
        if (intent == ClimbIntent.AUTOMATIC && approach * approach <= CLIMB_INPUT_EPSILON) {
            return null;
        }

        Vec3 groupPosition = collision.toGroup(structurePosition);
        double vertical = intent == ClimbIntent.DOWN
                ? -CLIMB_DESCENT_SPEED
                : Math.max(groupMovement.y, CLIMB_SPEED);
        double centering = Mth.clamp(climbableCenter(collision, climbable.approachAxis())
                        - (climbable.approachAxis() == Direction.Axis.X
                        ? groupPosition.z : groupPosition.x),
                -CLIMB_CENTERING_SPEED, CLIMB_CENTERING_SPEED);
        double guidedApproach = climbableApproachMovement(climbable, groupPosition, vertical);
        Vec3 climbing = climbable.approachAxis() == Direction.Axis.X
                ? new Vec3(guidedApproach, vertical, centering)
                : new Vec3(centering, vertical, guidedApproach);
        GUIDED_CLIMBS.put(entity,
                new GuidedClimb(new WeakReference<>(structure), entity.level().getGameTime()));
        if (structurePosition.y + entity.getBbHeight() * HAND_HEIGHT_FRACTION <= climbable.releaseY()) {
            rememberClimbPoseAttachment(entity, structure, 0.0F,
                    climberYawDegrees(structure, climbable));
        }
        return transform.directionToWorld(collision.fromGroupDirection(climbing));
    }

    private static double climbableCenter(CollisionGroup collision, Direction.Axis approachAxis) {
        double minimum = Double.POSITIVE_INFINITY;
        double maximum = Double.NEGATIVE_INFINITY;
        for (CollisionPart part : collision.parts()) {
            AABB box = part.box();
            for (double x : new double[]{box.minX, box.maxX}) {
                for (double z : new double[]{box.minZ, box.maxZ}) {
                    Vec3 point = part.pose().toStructure(new Vec3(x, box.getCenter().y, z));
                    double coordinate = approachAxis == Direction.Axis.X ? point.z : point.x;
                    minimum = Math.min(minimum, coordinate);
                    maximum = Math.max(maximum, coordinate);
                }
            }
        }
        return (minimum + maximum) * 0.5D;
    }

    private static double climbableApproachMovement(ClimbableGroup climbable, Vec3 groupPosition,
                                                     double vertical) {
        if (!Double.isFinite(climbable.approachAtPathTop())) {
            return 0.0D;
        }

        double targetY = groupPosition.y + vertical;
        double targetApproach;
        if (targetY <= climbable.pathTopY() || climbable.releaseY() == climbable.pathTopY()) {
            targetApproach = climbable.approachAtPathTop()
                    + (targetY - climbable.pathTopY()) * climbable.approachPerRise();
        } else {
            double landingProgress = Mth.clamp(
                    (targetY - climbable.pathTopY())
                            / (climbable.releaseY() - climbable.pathTopY()),
                    0.0D, 1.0D);
            targetApproach = Mth.lerp(landingProgress,
                    climbable.approachAtPathTop(), climbable.approachAtRelease());
        }
        double currentApproach = climbable.approachAxis() == Direction.Axis.X
                ? groupPosition.x
                : groupPosition.z;
        return Mth.clamp(targetApproach - currentApproach,
                -CLIMB_CENTERING_SPEED, CLIMB_CENTERING_SPEED);
    }

    private static boolean intersectsClimbable(CollidableStructure structure, ClimbableGroup climbable,
                                               AABB worldBox) {
        return intersectsClimbable(structure.collisionTransform(), climbable, worldBox);
    }

    private static boolean intersectsClimbable(StructureTransform transform, ClimbableGroup climbable,
                                               AABB worldBox) {
        return intersectsClimbable(transform, climbable, worldBox, CLIMBABLE_REACH);
    }

    private static boolean intersectsClimbable(StructureTransform transform, ClimbableGroup climbable,
                                               AABB worldBox, double reach) {
        return StructureCollisionResolver.intersects(transform,
                List.of(climbable.collision()), worldBox, reach);
    }

    static boolean intersectsAnyClimbable(CollidableStructure structure,
                                          StructureTransform transform, AABB worldBox,
                                          double reach) {
        for (ClimbableGroup climbable : structure.climbableGroups()) {
            if (intersectsClimbable(transform, climbable, worldBox, reach)) {
                return true;
            }
        }
        return false;
    }

    static boolean isNearAttachedClimbable(Entity entity, CollidableStructure structure,
                                           StructureTransform transform, AABB worldBox) {
        return hasRecentClimbAttachment(entity, structure)
                && intersectsAnyClimbable(
                structure, transform, worldBox, ATTACHED_CLIMBABLE_REACH);
    }

    static void rememberClimbAttachment(Entity entity, CollidableStructure structure) {
        // Attachment is deliberately broader than pose state: it also keeps an idle climber tied
        // to a moving ladder for a few ticks.
        CLIMB_ATTACHMENTS.put(entity,
                new ClimbAttachment(new WeakReference<>(structure), entity.level().getGameTime()));
    }
    private static float climberYawDegrees(CollidableStructure structure, ClimbableGroup climbable) {
        StructureTransform transform = structure.collisionTransform();
        CollisionGroup collision = climbable.collision();
        double facingSign = climbable.climbFacing() >= 0.0D ? 1.0D : -1.0D;

        Vec3 facing = transform.directionToWorld(collision.fromGroupDirection(
                climbable.approachAxis() == Direction.Axis.X
                        ? new Vec3(facingSign, 0.0D, 0.0D)
                        : new Vec3(0.0D, 0.0D, facingSign)));
        if (facing.horizontalDistanceSqr() < 1.0E-6D) return 0.0F;
        return (float) (Mth.atan2(facing.z, facing.x) * (180.0D / Math.PI)) - 90.0F;
    }
    /** Entry point for traversal implementations that do not use Axiomata climbable groups. */
    public static void markClimbPoseAttachment(Entity entity, CollidableStructure structure,
                                               float inclineRadians, float uphillYawDegrees) {
        rememberClimbPoseAttachment(entity, structure, inclineRadians, uphillYawDegrees);
    }

    private static void rememberClimbPoseAttachment(Entity entity, CollidableStructure structure,
                                                    float inclineRadians, float uphillYawDegrees) {
        if (!entity.level().isClientSide) {
            return;
        }
        long now = entity.level().getGameTime();
        ClimbPoseAttachment previous = CLIMB_POSE_ATTACHMENTS.get(entity);
        long startedAt = previous != null && previous.structure().get() == structure
                ? previous.startedAt()
                : now;
        CLIMB_POSE_ATTACHMENTS.put(entity,
                new ClimbPoseAttachment(new WeakReference<>(structure), startedAt, now,
                        Mth.clamp(Math.abs(inclineRadians), 0.0F, Mth.HALF_PI),
                        Mth.wrapDegrees(uphillYawDegrees)));
    }

    public static ClimbPoseState getClimbPoseState(Entity entity, float partialTick) {
        ClimbPoseAttachment attachment = CLIMB_POSE_ATTACHMENTS.get(entity);
        if (attachment == null || attachment.structure().get() == null) {
            return ClimbPoseState.INACTIVE;
        }
        double now = entity.level().getGameTime() + Mth.clamp(partialTick, 0.0F, 1.0F);
        double sinceTouched = now - attachment.touchedAt();
        if (sinceTouched < 0.0D) {
            return ClimbPoseState.INACTIVE;
        }
        double fadeOutStart = CLIMB_POSE_HOLD_TICKS;
        double expiry = fadeOutStart + CLIMB_POSE_FADE_OUT_TICKS;
        if (sinceTouched >= expiry) {
            CLIMB_POSE_ATTACHMENTS.remove(entity);
            return ClimbPoseState.INACTIVE;
        }

        float fadeIn = Mth.clamp((float) ((now - attachment.startedAt()) / CLIMB_POSE_FADE_IN_TICKS),
                0.0F, 1.0F);
        float fadeOut = sinceTouched <= fadeOutStart
                ? 1.0F
                : Mth.clamp((float) (1.0D - (sinceTouched - fadeOutStart) / CLIMB_POSE_FADE_OUT_TICKS),
                0.0F, 1.0F);
        return new ClimbPoseState(Math.min(fadeIn, fadeOut), attachment.inclineRadians(),
                attachment.uphillYawDegrees());
    }

    private static boolean hasRecentClimbAttachment(Entity entity, CollidableStructure structure) {
        ClimbAttachment attachment = CLIMB_ATTACHMENTS.get(entity);
        if (attachment == null || attachment.structure().get() != structure) {
            return false;
        }
        long age = entity.level().getGameTime() - attachment.touchedAt();
        if (age >= 0L && age <= CLIMB_CARRY_GRACE_TICKS) {
            return true;
        }
        CLIMB_ATTACHMENTS.remove(entity);
        return false;
    }

    static void clearClimbAttachment(Entity entity, CollidableStructure structure) {
        ClimbAttachment attachment = CLIMB_ATTACHMENTS.get(entity);
        if (attachment != null && attachment.structure().get() == structure) {
            CLIMB_ATTACHMENTS.remove(entity);
        }
    }

    private enum ClimbIntent {
        NONE,
        UP,
        DOWN,
        AUTOMATIC
    }

    private record ClimbAttachment(WeakReference<CollidableStructure> structure, long touchedAt) {
    }

    private record ClimbPoseAttachment(WeakReference<CollidableStructure> structure,
                                       long startedAt, long touchedAt, float inclineRadians,
                                       float uphillYawDegrees) {
    }

    public record ClimbPoseState(float weight, float inclineRadians, float uphillYawDegrees) {
        private static final ClimbPoseState INACTIVE = new ClimbPoseState(0.0F, 0.0F, 0.0F);
    }
}
