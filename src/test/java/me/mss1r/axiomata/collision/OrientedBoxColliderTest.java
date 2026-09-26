package me.mss1r.axiomata.collision;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrientedBoxColliderTest {
    private static final double EPSILON = 1.0E-7D;

    @Test
    void reportsContinuousTimeOfImpact() {
        AABB fixed = new AABB(0, 0, 0, 1, 1, 1);
        OrientedBox moving = new OrientedBox(
                new Vec3(2, 0.5, 0.5),
                new Vec3(0.5, 0.5, 0.5),
                Rotation3.IDENTITY);

        BoxSeparation separation = OrientedBoxCollider.separate(fixed, moving,
                new Vec3(-2, 0, 0));

        assertNotNull(separation);
        assertEquals(0.25D, separation.timeOfImpact(), EPSILON);
        assertEquals(new Vec3(1, 0, 0), separation.contactNormal());
    }

    @Test
    void pushesAnOverlappingBoxOutAlongTheShallowestAxis() {
        AABB fixed = new AABB(0, 0, 0, 1, 1, 1);
        OrientedBox moving = new OrientedBox(
                new Vec3(0.5, 1.25, 0.5),
                new Vec3(0.5, 0.5, 0.5),
                Rotation3.IDENTITY);

        BoxSeparation separation = OrientedBoxCollider.separate(fixed, moving, Vec3.ZERO);

        assertNotNull(separation);
        assertTrue(separation.isOverlapping());
        Vec3 push = separation.pushOut(0.0D);
        assertNotNull(push);
        assertTrue(push.y > 0.25D);
        assertEquals(0.0D, push.x, EPSILON);
        assertEquals(0.0D, push.z, EPSILON);
    }

    @Test
    void inverseRotationRestoresTheOriginalVector() {
        Rotation3 rotation = Rotation3.aroundY((float) Math.toRadians(37.0D))
                .multiply(Rotation3.aroundX((float) Math.toRadians(-18.0D)));
        Vec3 original = new Vec3(1.25D, -3.5D, 7.0D);
        Vec3 restored = rotation.transformInverse(rotation.transform(original));

        assertEquals(original.x, restored.x, EPSILON);
        assertEquals(original.y, restored.y, EPSILON);
        assertEquals(original.z, restored.z, EPSILON);
    }
}

