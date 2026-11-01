package me.mss1r.axiomata.collision;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureGeometryIntersectionTest {
    private static final CollisionShape UNIT_BOX = new CollisionShape(Vec3.ZERO, List.of(
            CollisionPart.axisAligned(new AABB(-0.5D, -0.5D, -0.5D, 0.5D, 0.5D, 0.5D))));
    private static final List<CollisionGroup> GROUPS = List.of(CollisionGroup.fixed("box", UNIT_BOX));

    @Test
    void detectsOverlapBetweenIndependentlyRotatedStructures() {
        TestStructure first = new TestStructure(new StructureTransform(0.0D, 0.0D, 0.0D, 45.0F));
        TestStructure overlapping = new TestStructure(new StructureTransform(0.9D, 0.0D, 0.0D, -20.0F));
        TestStructure separate = new TestStructure(new StructureTransform(2.0D, 0.0D, 0.0D, -20.0F));

        assertTrue(StructureCollisionResolver.intersects(first, GROUPS, overlapping, GROUPS, 0.0D));
        assertFalse(StructureCollisionResolver.intersects(first, GROUPS, separate, GROUPS, 0.0D));
    }

    @Test
    void reachTurnsNearContactIntoSupport() {
        TestStructure first = new TestStructure(new StructureTransform(0.0D, 0.0D, 0.0D, 0.0F));
        TestStructure near = new TestStructure(new StructureTransform(1.04D, 0.0D, 0.0D, 0.0F));

        assertFalse(StructureCollisionResolver.intersects(first, GROUPS, near, GROUPS, 0.0D));
        assertTrue(StructureCollisionResolver.intersects(first, GROUPS, near, GROUPS, 0.05D));
    }

    @Test
    void rotatedReachIncludesCornersBeyondWorldAxisPadding() {
        var tiny = List.of(CollisionGroup.fixed("tip", new CollisionShape(Vec3.ZERO, List.of(
                CollisionPart.axisAligned(new AABB(-.005, -.005, -.005, .005, .005, .005))))));
        var first = new TestStructure(new StructureTransform(1.5, 0, 0, 0));
        var second = new TestStructure(new StructureTransform(0, 0, 0, 45));
        assertTrue(StructureCollisionResolver.intersects(first, tiny, second, GROUPS, .6));
        assertFalse(StructureCollisionResolver.intersects(first, tiny, second, GROUPS, 0));
    }

    @Test
    void indexedIntersectionsMatchExhaustiveOrientedTests() {
        Random random = new Random(0x51E6E);
        int overlaps = 0, separate = 0;
        for (int sample = 0; sample < 160; sample++) {
            var first = new TestStructure(new StructureTransform(10_000, 64, -10_000, random.nextFloat() * 360 - 180));
            var second = new TestStructure(new StructureTransform(10_000 + random.nextDouble() * 12 - 6,
                    64 + random.nextDouble() * 4 - 2, -10_000 + random.nextDouble() * 12 - 6,
                    random.nextFloat() * 360 - 180));
            var firstGroups = randomGroups(random);
            var secondGroups = randomGroups(random);
            double reach = sample % 3 == 0 ? .6 : sample % 3 == 1 ? .03 : 0;
            boolean expected = exhaustive(first, firstGroups, second, secondGroups, reach);
            assertEquals(expected, StructureCollisionResolver.intersects(first, firstGroups, second, secondGroups, reach),
                    "Oriented compound sample " + sample);
            if (expected) overlaps++; else separate++;
        }
        assertTrue(overlaps > 0 && separate > 0, "Both contact and empty-space queries must be covered");
    }

    @Test
    void changingAnArticulatedPoseInvalidatesThePreparedIndex() {
        var first = new TestStructure(new StructureTransform(0, 0, 0, 0));
        var second = new TestStructure(new StructureTransform(0, 0, 0, 0));
        var parts = new ArrayList<CollisionPart>();
        for (int i = 0; i < 12; i++) parts.add(CollisionPart.axisAligned(new AABB(i * 3, -.5, -.5, i * 3 + 1, .5, .5)));
        var shape = new CollisionShape(Vec3.ZERO, parts);
        var groups = new ArrayList<>(List.of(CollisionGroup.fixed("mechanism", shape)));
        assertTrue(StructureCollisionResolver.intersects(first, GROUPS, second, groups, 0));
        groups.set(0, new CollisionGroup("mechanism", shape, CollisionPose.translated(new Vec3(0, 3, 0))));
        assertFalse(StructureCollisionResolver.intersects(first, GROUPS, second, groups, 0));
        groups.set(0, CollisionGroup.fixed("mechanism", shape));
        assertTrue(StructureCollisionResolver.intersects(first, GROUPS, second, groups, 0));
    }

    private static List<CollisionGroup> randomGroups(Random random) {
        var parts = new ArrayList<CollisionPart>();
        for (int i = 0; i < 14; i++) {
            Vec3 center = new Vec3(random.nextDouble() * 8 - 4, random.nextDouble() * 4 - 2, random.nextDouble() * 8 - 4);
            AABB box = new AABB(center, center.add(.2 + random.nextDouble(), .2 + random.nextDouble(), .2 + random.nextDouble()));
            parts.add(new CollisionPart(box, CollisionPose.aroundX(box.getCenter(), random.nextFloat() * 3)));
        }
        var shape = new CollisionShape(new Vec3(.3, .7, -.2), parts);
        return List.of(new CollisionGroup("articulated", shape,
                CollisionPose.aroundX(shape.pivot(), random.nextFloat() * 3)
                        .then(CollisionPose.aroundY(shape.pivot(), random.nextFloat() * 3))));
    }

    /** Independent exhaustive SAT reference, without prepared geometry or a broadphase. */
    private static boolean exhaustive(TestStructure first, List<CollisionGroup> firstGroups,
                                      TestStructure second, List<CollisionGroup> secondGroups, double reach) {
        var firstTransform = first.collisionTransform();
        var secondTransform = second.collisionTransform();
        for (var secondGroup : secondGroups) for (var secondPart : secondGroup.parts()) {
            Rotation3 worldToSecond = secondGroup.structureToPartRotation(secondPart).multiply(secondTransform.worldToLocalRotation());
            for (var firstGroup : firstGroups) for (var firstPart : firstGroup.parts()) {
                Vec3 center = firstTransform.toWorld(firstGroup.fromPart(firstPart, firstPart.box().getCenter()));
                Rotation3 rotation = worldToSecond.multiply(firstTransform.worldToLocalRotation().transpose())
                        .multiply(firstGroup.structureToPartRotation(firstPart).transpose());
                var box = new OrientedBox(secondGroup.toPart(secondPart, secondTransform.toLocal(center)),
                        OrientedBox.halfExtentOf(firstPart.box()), rotation);
                var separation = OrientedBoxCollider.separate(secondPart.box().inflate(Math.max(0, reach)), box, Vec3.ZERO);
                if (separation != null && separation.isOverlapping()) return true;
            }
        }
        return false;
    }

    private record TestStructure(StructureTransform collisionTransform) implements CollidableStructure {
        @Override
        public List<CollisionGroup> collisionGroups() {
            return GROUPS;
        }

        @Override
        public StructureTransform previousCollisionTransform() {
            return collisionTransform;
        }
    }
}
