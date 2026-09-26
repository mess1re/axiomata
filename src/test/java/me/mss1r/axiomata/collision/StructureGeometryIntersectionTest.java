package me.mss1r.axiomata.collision;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
