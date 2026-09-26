package me.mss1r.axiomata.collision;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class OrientedBoxCollider {
    private static final double AXIS_EPSILON = 1.0E-10D;
    private static final double CONTACT_EPSILON = 1.0E-7D;
    private static final Vec3[] FIXED_AXES = {
            new Vec3(1.0D, 0.0D, 0.0D),
            new Vec3(0.0D, 1.0D, 0.0D),
            new Vec3(0.0D, 0.0D, 1.0D)
    };

    private OrientedBoxCollider() {
    }

    public static BoxSeparation separate(AABB fixed, OrientedBox moving, Vec3 movement) {
        Rotation3 rotation = moving.rotation();
        Vec3 fixedExtent = OrientedBox.halfExtentOf(fixed);
        Vec3 movingExtent = moving.halfExtent();
        Vec3 offset = moving.center().subtract(fixed.getCenter());

        List<Vec3> axes = new ArrayList<>(15);
        for (Vec3 axis : FIXED_AXES) {
            axes.add(axis);
        }
        for (int index = 0; index < 3; index++) {
            axes.add(rotation.axis(index));
        }
        for (Vec3 fixedAxis : FIXED_AXES) {
            for (int index = 0; index < 3; index++) {
                Vec3 cross = fixedAxis.cross(rotation.axis(index));
                if (cross.lengthSqr() > AXIS_EPSILON) {
                    axes.add(cross.normalize());
                }
            }
        }

        BoxSeparation separation = new BoxSeparation(rotation.axis(1));
        boolean strictlyOverlapping = true;

        for (Vec3 rawAxis : axes) {
            Vec3 axis = rawAxis.normalize();
            double projectedOffset = offset.dot(axis);
            double projectedMovement = movement.dot(axis);
            double fixedReach = projectedExtent(fixedExtent, Rotation3.IDENTITY, axis);
            double movingReach = projectedExtent(movingExtent, rotation, axis);
            double combinedReach = fixedReach + movingReach;
            double gap = Math.abs(projectedOffset) - combinedReach;

            if (gap >= -CONTACT_EPSILON) {
                strictlyOverlapping = false;
            } else {
                Vec3 normal = outwardNormal(axis, projectedOffset, projectedMovement);
                separation.recordShallowest(normal, -gap);
                recordStepDepth(separation, axis, projectedOffset, combinedReach);
            }

            if (Math.abs(projectedMovement) <= AXIS_EPSILON) {
                if (gap > CONTACT_EPSILON) {
                    return null;
                }
                continue;
            }

            double first = (-combinedReach - projectedOffset) / projectedMovement;
            double second = (combinedReach - projectedOffset) / projectedMovement;
            double entry = Math.min(first, second);
            double exit = Math.max(first, second);
            if (exit < 0.0D || entry > 1.0D) {
                return null;
            }

            separation.recordExit(exit);
            if (gap >= -CONTACT_EPSILON) {
                double contactOffset = projectedOffset + projectedMovement * Math.max(0.0D, entry);
                separation.recordEntry(entry, outwardNormal(axis, contactOffset, projectedMovement));
            }
        }

        if (strictlyOverlapping) {
            separation.markOverlapping();
            return separation;
        }
        return separation.timeOfImpact() == BoxSeparation.NO_IMPACT ? null : separation;
    }

    private static double projectedExtent(Vec3 extent, Rotation3 rotation, Vec3 axis) {
        return extent.x * Math.abs(rotation.axis(0).dot(axis))
                + extent.y * Math.abs(rotation.axis(1).dot(axis))
                + extent.z * Math.abs(rotation.axis(2).dot(axis));
    }

    private static Vec3 outwardNormal(Vec3 axis, double offset, double movement) {
        double side = Math.abs(offset) > CONTACT_EPSILON
                ? Math.signum(offset)
                : (movement == 0.0D ? 1.0D : -Math.signum(movement));
        return axis.scale(side);
    }

    private static void recordStepDepth(BoxSeparation separation, Vec3 axis,
                                        double offset, double combinedReach) {
        double alignment = separation.stepAxis().dot(axis);
        if (Math.abs(alignment) <= AXIS_EPSILON) {
            return;
        }
        double target = Math.signum(alignment) * combinedReach;
        separation.recordStep((target - offset) / alignment);
    }
}
