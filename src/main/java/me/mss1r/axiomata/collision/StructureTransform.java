package me.mss1r.axiomata.collision;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public record StructureTransform(double x, double y, double z, float yawDegrees) {
    public Vec3 toWorld(Vec3 local) {
        Vec3 rotated = directionToWorld(local);
        return new Vec3(x + rotated.x, y + rotated.y, z + rotated.z);
    }

    public Vec3 toLocal(Vec3 world) {
        return directionToLocal(new Vec3(world.x - x, world.y - y, world.z - z));
    }

    public Vec3 directionToWorld(Vec3 local) {
        return worldToLocalRotation().transformInverse(local);
    }

    public Vec3 directionToLocal(Vec3 world) {
        return worldToLocalRotation().transform(world);
    }

    Rotation3 worldToLocalRotation() {
        return Rotation3.aroundY(yawDegrees * Mth.DEG_TO_RAD);
    }

    public Vec3 carryFrom(StructureTransform previous, Vec3 world) {
        return toWorld(previous.toLocal(world)).subtract(world);
    }
}
