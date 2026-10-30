package me.mss1r.axiomata.ballistics;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Shared projectile-opening test; loader adapters decide how to skip the impact. */
public final class ProjectilePassageHandler {
    private ProjectilePassageHandler() {}
    public static boolean allowsPassage(ProjectilePassThroughControl target, Vec3 start, Vec3 movement, Vec3 hit) {
        Vec3 end = movement.lengthSqr() > 1.0E-6D ? start.add(movement) : hit;
        return target.allowsProjectilePassage(start, end);
    }
}
