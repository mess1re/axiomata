package me.mss1r.axiomata.collision;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StructureCarryTransformTest {
    private static final CollisionShape SHAPE = new CollisionShape(Vec3.ZERO,
            List.of(CollisionPart.axisAligned(new AABB(-5, -.25, -5, 5, 0, 5))));
    private static final CollisionGroup DECK = CollisionGroup.fixed("deck", SHAPE);
    private static final StructureTransform ORIGIN = new StructureTransform(0, 0, 0, 0);

    @Test void translatesWithoutChangingHeading() {
        var carried = new StructureTransform(2, 0, 1, 35);
        var moved = StructureCarryTransform.carry(ORIGIN, DECK, new StructureTransform(3, 4, 5, 0), DECK, carried);
        assertEquals(5, moved.x(), 1e-6);
        assertEquals(4, moved.y(), 1e-6);
        assertEquals(6, moved.z(), 1e-6);
        assertEquals(35, moved.yawDegrees(), 1e-5);
    }

    @Test void rotatesPositionAndHeadingAroundSupportNotWorldOrigin() {
        var previous = new StructureTransform(10, 0, 10, 0);
        var current = new StructureTransform(10, 0, 10, 90);
        var moved = StructureCarryTransform.carry(previous, DECK, current, DECK, new StructureTransform(10, 0, 12, 20));
        assertEquals(8, moved.x(), 1e-6);
        assertEquals(10, moved.z(), 1e-6);
        assertEquals(110, moved.yawDegrees(), 1e-5);
    }

    @Test void followsAnIndependentlyMovingSupportGroup() {
        var raised = new CollisionGroup("deck", SHAPE, CollisionPose.translated(new Vec3(0, 2, 0)));
        var moved = StructureCarryTransform.carry(ORIGIN, DECK, ORIGIN, raised, new StructureTransform(0, 0, 1, 0));
        assertEquals(2, moved.y(), 1e-6);
    }

    @Test void aTiltedSupportCarriesTheContactPointButKeepsTheEntityUpright() {
        var tilted = new CollisionGroup("deck", SHAPE, CollisionPose.aroundX(Vec3.ZERO, (float)(Math.PI / 6)));
        var moved = StructureCarryTransform.carry(ORIGIN, DECK, ORIGIN, tilted, new StructureTransform(0, 0, 2, 0));
        assertEquals(-1, moved.y(), 1e-6);
        assertEquals(Math.sqrt(3), moved.z(), 1e-6);
        assertEquals(0, moved.yawDegrees(), 1e-6);
    }

    @Test void consumingTheSameSupportPoseTwiceDoesNotMoveCargoAgain() {
        var current = new StructureTransform(3, 1, 4, 30);
        var carried = StructureCarryTransform.carry(ORIGIN, DECK, current, DECK, new StructureTransform(2, 0, 1, 5));
        var again = StructureCarryTransform.carry(current, DECK, current, DECK, carried);
        assertEquals(carried.x(), again.x(), 1e-6);
        assertEquals(carried.y(), again.y(), 1e-6);
        assertEquals(carried.z(), again.z(), 1e-6);
        assertEquals(carried.yawDegrees(), again.yawDegrees(), 1e-5);
    }
}
