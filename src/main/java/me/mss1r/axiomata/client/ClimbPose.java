package me.mss1r.axiomata.client;

import me.mss1r.axiomata.collision.system.StructureClimbingSystem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Humanoid pose for an entity on a structure ladder: hands on the rungs, the body leaning with the
 * ladder, arms and legs stepping with the height climbed. Every structure that declares climbable
 * groups gets it; it is driven only by {@link StructureClimbingSystem#getClimbPoseState}.
 */
public final class ClimbPose {
    private static final float CYCLE_SPEED = 2.0F;
    private static final float CYCLE_AMPLITUDE = 0.35F;
    private static final float MIN_LEG_CYCLE = 0.25F;
    private static final float HEAD_MAX_YAW = 70.0F;
    private static final float HEAD_MAX_PITCH = 55.0F;
    private static final Rotation RIGHT_ARM = Rotation.degrees(-97.40283F, -11.45916F, 17.18873F);
    private static final Rotation LEFT_ARM = Rotation.degrees(-97.40283F, 11.45916F, -17.18873F);
    private static final Rotation RIGHT_LEG = Rotation.degrees(-52.0F, -9.16733F, 13.75098F);
    private static final Rotation LEFT_LEG = Rotation.degrees(-52.0F, 9.16733F, -13.75098F);

    private ClimbPose() {
    }

    /** Returns whether a pose was applied, so the caller knows to reset the parts next frame. */
    public static boolean apply(HumanoidModel<?> model, LivingEntity entity, float age,
                                float headYaw, float headPitch) {
        if (entity.getVehicle() != null) {
            return false;
        }
        float partialTick = Mth.clamp(age - entity.tickCount, 0.0F, 1.0F);
        StructureClimbingSystem.ClimbPoseState state =
                StructureClimbingSystem.getClimbPoseState(entity, partialTick);
        if (state.weight() <= 0.0F) {
            return false;
        }
        Snapshot before = state.weight() < 1.0F ? Snapshot.capture(model) : null;

        float incline = Mth.clamp(state.inclineRadians(), 0.0F, Mth.HALF_PI);
        Rotation upper = new Rotation(incline, 0.0F, 0.0F);
        PartPose rightHip = model.rightLeg.getInitialPose();
        PartPose leftHip = model.leftLeg.getInitialPose();
        float hipX = (rightHip.x + leftHip.x) * 0.5F;
        float hipY = (rightHip.y + leftHip.y) * 0.5F;
        float hipZ = (rightHip.z + leftHip.z) * 0.5F;
        placeAroundHip(model.head, upper, Rotation.ZERO, hipX, hipY, hipZ);
        placeAroundHip(model.body, upper, Rotation.ZERO, hipX, hipY, hipZ);
        placeAroundHip(model.rightArm, upper, RIGHT_ARM, hipX, hipY, hipZ);
        placeAroundHip(model.leftArm, upper, LEFT_ARM, hipX, hipY, hipZ);
        placeAroundHip(model.rightLeg, upper, RIGHT_LEG, hipX, hipY, hipZ);
        placeAroundHip(model.leftLeg, upper, LEFT_LEG, hipX, hipY, hipZ);

        // Hands and feet alternate with height climbed, not with time, so a climber at rest is still.
        float height = Mth.lerp(partialTick, (float) entity.yo, (float) entity.getY());
        float cycle = -Mth.cos(height * CYCLE_SPEED) * CYCLE_AMPLITUDE;
        model.rightArm.xRot -= cycle;
        model.leftArm.xRot += cycle;
        float legCycle = cycle * Mth.lerp(Mth.cos(incline), MIN_LEG_CYCLE, 1.0F);
        model.rightLeg.xRot -= legCycle;
        model.leftLeg.xRot += legCycle;

        // The body faces the ladder; the head may still look around within a comfortable range.
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float correction = Mth.wrapDegrees(state.uphillYawDegrees() - bodyYaw);
        float lookYaw = foldLookYaw(Mth.wrapDegrees(headYaw - correction), HEAD_MAX_YAW);
        model.head.xRot = Mth.clamp(headPitch, -HEAD_MAX_PITCH, HEAD_MAX_PITCH) * Mth.DEG_TO_RAD;
        model.head.yRot = lookYaw * Mth.DEG_TO_RAD;
        model.head.zRot = 0.0F;

        if (before != null) {
            before.blendInto(model, state.weight());
        }
        model.hat.copyFrom(model.head);
        return true;
    }

    public static void reset(HumanoidModel<?> model) {
        model.head.resetPose();
        model.hat.resetPose();
        model.body.resetPose();
        model.rightArm.resetPose();
        model.leftArm.resetPose();
        model.rightLeg.resetPose();
        model.leftLeg.resetPose();
    }

    private static void placeAroundHip(ModelPart part, Rotation upper, Rotation local,
                                       float hipX, float hipY, float hipZ) {
        PartPose vanilla = part.getInitialPose();
        float[] p = upper.rotate(vanilla.x - hipX, vanilla.y - hipY, vanilla.z - hipZ);
        part.setPos(p[0] + hipX, p[1] + hipY, p[2] + hipZ);
        Rotation combined = upper.then(local);
        part.setRotation(combined.x, combined.y, combined.z);
    }

    /** Looking further back than the limit folds the head forward again instead of snapping. */
    private static float foldLookYaw(float yaw, float maxYaw) {
        float magnitude = Math.abs(yaw);
        if (magnitude <= maxYaw) {
            return yaw;
        }
        float remaining = (180.0F - magnitude) / (180.0F - maxYaw);
        return Math.copySign(maxYaw * Mth.clamp(remaining, 0.0F, 1.0F), yaw);
    }

    /** Model-part angles in the Z*Y*X order model parts apply them. */
    private record Rotation(float x, float y, float z) {
        static final Rotation ZERO = new Rotation(0.0F, 0.0F, 0.0F);

        static Rotation degrees(float x, float y, float z) {
            return new Rotation(x * Mth.DEG_TO_RAD, y * Mth.DEG_TO_RAD, z * Mth.DEG_TO_RAD);
        }

        float[] matrix() {
            float sx = Mth.sin(x), cx = Mth.cos(x);
            float sy = Mth.sin(y), cy = Mth.cos(y);
            float sz = Mth.sin(z), cz = Mth.cos(z);
            return new float[] {
                    cz * cy, cz * sy * sx - sz * cx, cz * sy * cx + sz * sx,
                    sz * cy, sz * sy * sx + cz * cx, sz * sy * cx - cz * sx,
                    -sy, cy * sx, cy * cx
            };
        }

        float[] rotate(float px, float py, float pz) {
            float[] m = matrix();
            return new float[] {
                    m[0] * px + m[1] * py + m[2] * pz,
                    m[3] * px + m[4] * py + m[5] * pz,
                    m[6] * px + m[7] * py + m[8] * pz
            };
        }

        /** A child rotation inside this parent, as one set of part angles. */
        Rotation then(Rotation child) {
            float[] a = matrix(), b = child.matrix(), m = new float[9];
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    m[row * 3 + column] = a[row * 3] * b[column]
                            + a[row * 3 + 1] * b[3 + column]
                            + a[row * 3 + 2] * b[6 + column];
                }
            }
            float yaw = (float) Math.asin(Mth.clamp(-m[6], -1.0F, 1.0F));
            if (Math.abs(Mth.cos(yaw)) > 1.0E-6F) {
                return new Rotation((float) Math.atan2(m[7], m[8]), yaw, (float) Math.atan2(m[3], m[0]));
            }
            return new Rotation((float) Math.atan2(-m[5], m[4]), yaw, 0.0F);
        }
    }

    /** The pose before climbing, so fading in and out blends instead of popping. */
    private record Snapshot(float[] head, float[] body, float[] rightArm, float[] leftArm,
                            float[] rightLeg, float[] leftLeg) {
        static Snapshot capture(HumanoidModel<?> model) {
            return new Snapshot(capture(model.head), capture(model.body), capture(model.rightArm),
                    capture(model.leftArm), capture(model.rightLeg), capture(model.leftLeg));
        }

        static float[] capture(ModelPart part) {
            return new float[] {part.x, part.y, part.z, part.xRot, part.yRot, part.zRot};
        }

        void blendInto(HumanoidModel<?> model, float weight) {
            blend(model.head, head, weight);
            blend(model.body, body, weight);
            blend(model.rightArm, rightArm, weight);
            blend(model.leftArm, leftArm, weight);
            blend(model.rightLeg, rightLeg, weight);
            blend(model.leftLeg, leftLeg, weight);
        }

        static void blend(ModelPart part, float[] from, float weight) {
            part.x = Mth.lerp(weight, from[0], part.x);
            part.y = Mth.lerp(weight, from[1], part.y);
            part.z = Mth.lerp(weight, from[2], part.z);
            part.xRot = Mth.lerp(weight, from[3], part.xRot);
            part.yRot = Mth.lerp(weight, from[4], part.yRot);
            part.zRot = Mth.lerp(weight, from[5], part.zRot);
        }
    }
}
