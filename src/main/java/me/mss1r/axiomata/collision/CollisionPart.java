package me.mss1r.axiomata.collision;

import net.minecraft.world.phys.AABB;

public record CollisionPart(AABB box, CollisionPose pose) {
    public static CollisionPart axisAligned(AABB box) {
        return new CollisionPart(box, CollisionPose.IDENTITY);
    }
}
