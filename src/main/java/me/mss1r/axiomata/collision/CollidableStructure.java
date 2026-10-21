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

    /**
     * Additional, contact-only geometry used when this structure is a load: placement, support
     * contact and space reserved by an unfinished build. Solid parts are included automatically.
     * Include a non-solid footing here when
     * e.g. a wheeled machine stands on its vanilla box, or a climb-through ladder rests on a deck.
     * These parts do not become obstacles for walkers or support surfaces for other loads.
     */
    default List<CollisionGroup> supportContactGroups() {
        return List.of();
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

    /**
     * Whether {@code Entity.move} resolves the solid geometry against the world and steps it over terrain. Return
     * false to keep vanilla movement by the bounding box, e.g. when the consumer clamps the geometry itself and the
     * box stands for the wheels the structure rolls on.
     */
    default boolean movesByGeometry() {
        return true;
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
