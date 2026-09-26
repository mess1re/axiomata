package me.mss1r.axiomata.collision;

import net.minecraft.world.phys.Vec3;

public record CollisionPose(Rotation3 rotation, Vec3 translation) {
    public static final CollisionPose IDENTITY = new CollisionPose(Rotation3.IDENTITY, Vec3.ZERO);

    public static CollisionPose translated(Vec3 translation) {
        return translation.equals(Vec3.ZERO)
                ? IDENTITY
                : new CollisionPose(Rotation3.IDENTITY, translation);
    }

    public static CollisionPose aroundX(Vec3 pivot, float radians) {
        if (radians == 0.0F) {
            return IDENTITY;
        }
        return around(pivot, Rotation3.aroundX(radians));
    }

    public static CollisionPose aroundY(Vec3 pivot, float radians) {
        if (radians == 0.0F) {
            return IDENTITY;
        }
        return around(pivot, Rotation3.aroundY(radians));
    }

    private static CollisionPose around(Vec3 pivot, Rotation3 rotation) {
        return new CollisionPose(rotation, pivot.subtract(rotation.transform(pivot)));
    }

    // Exported geometry is turned half a revolution around Y, which reverses GeckoLib's X axis.
    public static CollisionPose fromGeckoBoneX(Vec3 pivot, float geckoRadians) {
        return aroundX(pivot, -geckoRadians);
    }

    public static CollisionPose fromGeckoBoneY(Vec3 pivot, float geckoRadians) {
        return aroundY(pivot, geckoRadians);
    }

    public static CollisionPose fromGeckoBonePosition(Vec3 modelPosition) {
        // GeckoLib positions are model pixels. Exported entity-local X/Z point the other way.
        return translated(new Vec3(
                -modelPosition.x / 16.0D,
                modelPosition.y / 16.0D,
                -modelPosition.z / 16.0D));
    }

    public Vec3 toStructure(Vec3 groupLocal) {
        return rotation.transform(groupLocal).add(translation);
    }

    public Vec3 toGroup(Vec3 structureLocal) {
        return rotation.transformInverse(structureLocal.subtract(translation));
    }

    public Vec3 directionToStructure(Vec3 groupLocal) {
        return rotation.transform(groupLocal);
    }

    public Vec3 directionToGroup(Vec3 structureLocal) {
        return rotation.transformInverse(structureLocal);
    }

    public CollisionPose then(CollisionPose next) {
        return new CollisionPose(
                next.rotation.multiply(rotation),
                next.rotation.transform(translation).add(next.translation));
    }

    public Rotation3 structureToGroupRotation() {
        return rotation.transpose();
    }

    public boolean isIdentity() {
        return equals(IDENTITY);
    }
}
