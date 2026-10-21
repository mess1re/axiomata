package me.mss1r.axiomata.collision;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.junit.jupiter.api.Test;

import java.util.*;

class CompoundBroadphaseTest {
    @Test
    void preparedGeometryInvalidatesOnPartReplacementAndRootMotion() {
        List<CollisionPart> parts =
                new ArrayList<>(List.of(CollisionPart.axisAligned(new AABB(0, 0, 0, 1, 1, 1))));
        var groups =
                List.of(
                        CollisionGroup.fixed(
                                "mutable-consumer", new CollisionShape(Vec3.ZERO, parts)));
        var origin = new StructureTransform(0, 0, 0, 0);
        assertEquals(0, StructureCollisionResolver.worldBounds(origin, groups).minX);
        parts.set(0, CollisionPart.axisAligned(new AABB(10, 0, 0, 11, 1, 1)));
        assertEquals(10, StructureCollisionResolver.worldBounds(origin, groups).minX);
        assertEquals(
                15,
                StructureCollisionResolver.worldBounds(new StructureTransform(5, 0, 0, 0), groups)
                        .minX);
        parts.clear();
        assertNull(StructureCollisionResolver.worldBounds(origin, groups));
    }

    @Test
    void indexedCompoundDepenetrationMatchesTheOrderedSingleBodySolver() throws Exception {
        var random = new Random(939);
        List<CollisionPart> parts = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            double x = random.nextDouble() * 8,
                    y = random.nextDouble() * 3,
                    z = random.nextDouble() * 8;
            parts.add(
                    new CollisionPart(
                            new AABB(x, y, z, x + 1.2, y + .7, z + 1.1),
                            CollisionPose.aroundY(
                                    new Vec3(x, y, z), (float) (random.nextDouble() * .6))));
        }
        var fixed = List.of(CollisionGroup.fixed("compound", new CollisionShape(Vec3.ZERO, parts)));
        var transform = new StructureTransform(0, 0, 0, 23);
        for (int i = 0; i < 200; i++) {
            Vec3 centre =
                    transform.toWorld(
                            new Vec3(
                                    random.nextDouble() * 8,
                                    random.nextDouble() * 4,
                                    random.nextDouble() * 8));
            AABB box =
                    new AABB(
                            centre.x - .35,
                            centre.y,
                            centre.z - .35,
                            centre.x + .35,
                            centre.y + 1,
                            centre.z + .35);
            Vec3 motion =
                    new Vec3(random.nextDouble() * .4 - .2, -.04, random.nextDouble() * .4 - .2);
            var expected = unindexed(transform, fixed, box, motion);
            var single = StructureCollisionResolver.resolve(transform, fixed, box, motion, 0);
            assertEquals(expected.pushOut(), single.pushOut(), "single-body push sample " + i);
            assertEquals(
                    expected.allowedTime(),
                    single.allowedTime(),
                    1e-8,
                    "single-body sweep sample " + i);
            var moving =
                    List.of(
                            CollisionGroup.fixed(
                                    "body",
                                    new CollisionShape(
                                            Vec3.ZERO, List.of(CollisionPart.axisAligned(box)))));
            var actual =
                    StructureCollisionResolver.resolveStructures(
                            transform, fixed, new StructureTransform(0, 0, 0, 0), moving, motion);
            assertEquals(
                    0, expected.pushOut().distanceTo(actual.pushOut()), 1e-8, "push sample " + i);
            assertEquals(expected.allowedTime(), actual.allowedTime(), 1e-8, "sweep sample " + i);
            assertEquals(expected.isSupported(), actual.isSupported(), "support sample " + i);
        }
    }

    private static StructureCollisionResolver.Response unindexed(
            StructureTransform transform, List<CollisionGroup> groups, AABB box, Vec3 motion)
            throws Exception {
        // Keep an independent, full ordered traversal as the regression oracle.
        var prepare =
                StructureCollisionResolver.class.getDeclaredMethod(
                        "preparedGeometry", StructureTransform.class, List.class);
        prepare.setAccessible(true);
        Object geometry = prepare.invoke(null, transform, groups);
        var field = geometry.getClass().getDeclaredField("parts");
        field.setAccessible(true);
        var solve =
                StructureCollisionResolver.class.getDeclaredMethod(
                        "resolvePrepared",
                        List.class,
                        CollisionBoxIndex.class,
                        OrientedBox.class,
                        Vec3.class,
                        double.class);
        solve.setAccessible(true);
        return (StructureCollisionResolver.Response)
                solve.invoke(
                        null,
                        field.get(geometry),
                        null,
                        OrientedBox.of(box, Rotation3.IDENTITY),
                        motion,
                        0D);
    }
}
