package me.mss1r.axiomata.collision;

import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record CollisionShape(Vec3 pivot, List<CollisionPart> parts) {
    public AABB enclosingBounds() {
        AABB bounds = null;
        for (CollisionPart part : parts) {
            AABB box = part.box();
            for (double x : new double[]{box.minX, box.maxX}) {
                for (double y : new double[]{box.minY, box.maxY}) {
                    for (double z : new double[]{box.minZ, box.maxZ}) {
                        Vec3 point = part.pose().toStructure(new Vec3(x, y, z));
                        AABB pointBounds = new AABB(point, point);
                        bounds = bounds == null ? pointBounds : bounds.minmax(pointBounds);
                    }
                }
            }
        }
        return bounds;
    }
}
