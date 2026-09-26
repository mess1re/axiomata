package me.mss1r.axiomata.collision;

import java.util.List;
import net.minecraft.world.phys.Vec3;

public record CollisionGroup(String name, CollisionShape shape, CollisionPose pose) {
    public CollisionGroup(String name, CollisionShape shape, float pitchRadians) {
        this(name, shape, CollisionPose.aroundX(shape.pivot(), pitchRadians));
    }

    public static CollisionGroup fixed(String name, CollisionShape shape) {
        return new CollisionGroup(name, shape, CollisionPose.IDENTITY);
    }

    public List<CollisionPart> parts() {
        return shape.parts();
    }

    public boolean isFixed() {
        return pose.isIdentity();
    }

    public Vec3 toGroup(Vec3 structureLocal) {
        return pose.toGroup(structureLocal);
    }

    public Vec3 fromGroup(Vec3 boneLocal) {
        return pose.toStructure(boneLocal);
    }

    public Vec3 toGroupDirection(Vec3 structureLocalDirection) {
        return pose.directionToGroup(structureLocalDirection);
    }

    public Vec3 fromGroupDirection(Vec3 boneLocalDirection) {
        return pose.directionToStructure(boneLocalDirection);
    }

    public Vec3 toPart(CollisionPart part, Vec3 structureLocal) {
        return part.pose().toGroup(toGroup(structureLocal));
    }

    public Vec3 fromPart(CollisionPart part, Vec3 partLocal) {
        return fromGroup(part.pose().toStructure(partLocal));
    }

    public Rotation3 structureToPartRotation(CollisionPart part) {
        return part.pose().structureToGroupRotation().multiply(pose.structureToGroupRotation());
    }
}
