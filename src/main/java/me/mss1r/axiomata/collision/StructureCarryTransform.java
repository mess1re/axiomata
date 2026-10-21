package me.mss1r.axiomata.collision;

import net.minecraft.world.phys.Vec3;

/** A supported entity stays upright, but follows its support point and heading. */
public final class StructureCarryTransform {
    private StructureCarryTransform() {
    }

    public static StructureTransform carry(
            StructureTransform previous, CollisionGroup previousGroup,
            StructureTransform current, CollisionGroup currentGroup, StructureTransform carried) {
        Vec3 point = previousGroup.toGroup(previous.toLocal(new Vec3(carried.x(), carried.y(), carried.z())));
        Vec3 position = current.toWorld(currentGroup.fromGroup(point));
        Vec3 heading = carried.directionToWorld(new Vec3(0, 0, 1));
        Vec3 localHeading = previousGroup.toGroupDirection(previous.directionToLocal(heading));
        Vec3 worldHeading = current.directionToWorld(currentGroup.fromGroupDirection(localHeading));
        float yaw = worldHeading.horizontalDistanceSqr() < 1.0E-10D ? carried.yawDegrees()
                : (float) Math.toDegrees(Math.atan2(-worldHeading.x, worldHeading.z));
        return new StructureTransform(position.x, position.y, position.z, yaw);
    }
}
