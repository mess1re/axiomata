package me.mss1r.axiomata.collision;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public interface CollidableStructure {
    List<CollisionGroup> collisionGroups();

    /** May exclude authored geometry used only for picking, projectiles or climbing. */
    default List<CollisionGroup> solidCollisionGroups() {
        return collisionGroups();
    }

    StructureTransform collisionTransform();

    /** Previous tick's root transform, used to carry entities instead of teleporting under them. */
    StructureTransform previousCollisionTransform();

    /** Previous tick's posed groups. Return a snapshot when parts can animate independently. */
    default List<CollisionGroup> previousCollisionGroups() {
        return collisionGroups();
    }

    default List<CollisionGroup> previousSolidCollisionGroups() {
        return previousCollisionGroups();
    }

    /**
     * Step-up height the structure's authored stairs need. Walkers use the larger of their own
     * step height and this, so a deck's steps can be a little taller than a vanilla block step.
     */
    default double walkStepHeight() {
        return 0.0D;
    }

    default List<ClimbableGroup> climbableGroups() {
        return List.of();
    }

    /** Override when the consumer stores its root heading separately from Entity rotation. */
    default void rotateWithSupport(float yawDelta) {
        if (this instanceof Entity entity) {
            entity.setYRot(entity.getYRot() + yawDelta);
            if (entity instanceof LivingEntity living) {
                living.setYBodyRot(living.yBodyRot + yawDelta);
                living.setYHeadRot(living.yHeadRot + yawDelta);
            }
        }
    }
}
