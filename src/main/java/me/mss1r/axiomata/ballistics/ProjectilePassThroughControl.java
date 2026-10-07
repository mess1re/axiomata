package me.mss1r.axiomata.ballistics;

import net.minecraft.world.phys.Vec3;

/** Lets an entity keep openings that projectiles can pass through. */
public interface ProjectilePassThroughControl {
    /** The points are in world space. Returning true ignores this collision. */
    boolean allowsProjectilePassage(Vec3 start, Vec3 end);
}
