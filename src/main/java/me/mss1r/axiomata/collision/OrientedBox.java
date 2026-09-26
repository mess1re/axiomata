package me.mss1r.axiomata.collision;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record OrientedBox(Vec3 center, Vec3 halfExtent, Rotation3 rotation) {
    public static OrientedBox of(AABB box, Rotation3 rotation) {
        return new OrientedBox(box.getCenter(), halfExtentOf(box), rotation);
    }

    public static Vec3 halfExtentOf(AABB box) {
        return new Vec3(box.getXsize() * 0.5D, box.getYsize() * 0.5D, box.getZsize() * 0.5D);
    }

    public OrientedBox movedTo(Vec3 newCenter) {
        return new OrientedBox(newCenter, halfExtent, rotation);
    }

    public OrientedBox moved(Vec3 offset) {
        return movedTo(center.add(offset));
    }

    public AABB enclosingBounds() {
        double x = Math.abs(rotation.m00() * halfExtent.x)
                + Math.abs(rotation.m01() * halfExtent.y)
                + Math.abs(rotation.m02() * halfExtent.z);
        double y = Math.abs(rotation.m10() * halfExtent.x)
                + Math.abs(rotation.m11() * halfExtent.y)
                + Math.abs(rotation.m12() * halfExtent.z);
        double z = Math.abs(rotation.m20() * halfExtent.x)
                + Math.abs(rotation.m21() * halfExtent.y)
                + Math.abs(rotation.m22() * halfExtent.z);
        return new AABB(center.x - x, center.y - y, center.z - z, center.x + x, center.y + y, center.z + z);
    }
}
