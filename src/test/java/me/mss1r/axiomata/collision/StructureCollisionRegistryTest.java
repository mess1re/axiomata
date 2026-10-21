package me.mss1r.axiomata.collision;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

class StructureCollisionRegistryTest {
    private static final CollisionShape CRATE =
            new CollisionShape(Vec3.ZERO, List.of(CollisionPart.axisAligned(new AABB(-1, 0, -1, 1, 1, 1))));
    private final List<Hull> hulls = new ArrayList<>();
    private Level level;

    @BeforeEach
    void world() {
        level = level();
    }

    @AfterEach
    void cleanup() {
        hulls.forEach(StructureCollisionRegistry::unregister);
    }

    private static Level level() {
        Level level = mock(Level.class);
        when(level.getProfiler()).thenReturn(InactiveProfiler.INSTANCE);
        return level;
    }

    private Hull hull(Level level, CollisionShape shape, double x, double z) {
        Hull hull = new Hull(level, shape);
        hull.setPos(x, 64, z);
        hulls.add(hull);
        StructureCollisionRegistry.register(hull);
        return hull;
    }

    private static AABB around(double x, double z) {
        return new AABB(x - .5, 64, z - .5, x + .5, 65, z + .5);
    }

    @Test
    void aQueryOnlyMeetsStructuresAtItsPlace() {
        Hull probe = hull(level, CRATE, -1000, -1000);
        for (int i = 0; i < 100; i++) hull(level, CRATE, i * 20, 0);
        assertEquals(List.of(hulls.get(26)), StructureCollisionRegistry.structuresNear(probe, around(500, 0), .01));
        // Bounds straddling a chunk column edge are found from either side.
        assertEquals(List.of(hulls.get(5)), StructureCollisionRegistry.structuresNear(probe, around(79.5, 0), .01));
        assertTrue(StructureCollisionRegistry.structuresNear(probe, around(510, 0), .01).isEmpty());
    }

    @Test
    void aMovedStructureIsFoundWhereItNowIs() {
        Hull probe = hull(level, CRATE, -1000, -1000);
        Hull moving = hull(level, CRATE, 0, 0);
        assertEquals(List.of(moving), StructureCollisionRegistry.structuresNear(probe, around(0, 0), .01));
        moving.setPos(300, 64, 300);
        StructureCollisionRegistry.register(moving);
        assertTrue(StructureCollisionRegistry.structuresNear(probe, around(0, 0), .01).isEmpty());
        assertEquals(List.of(moving), StructureCollisionRegistry.structuresNear(probe, around(300, 300), .01));
    }

    @Test
    void neighboursComeInRegistrationOrderAndNeverIncludeTheAsker() {
        Hull first = hull(level, CRATE, 0, 0);
        Hull second = hull(level, CRATE, 1, 0);
        Hull third = hull(level, CRATE, -1, 0);
        StructureCollisionRegistry.register(first);
        AABB all = new AABB(-3, 64, -3, 3, 65, 3);
        assertEquals(List.of(first, third), StructureCollisionRegistry.structuresNear(second, all, .01));
    }

    @Test
    void removedAndForeignWorldStructuresAreNotNeighbours() {
        Hull probe = hull(level, CRATE, -1000, -1000);
        Hull removed = hull(level, CRATE, 0, 0);
        Hull elsewhere = hull(level(), CRATE, 0, 0);
        removed.discard();
        assertTrue(StructureCollisionRegistry.structuresNear(probe, around(0, 0), .01).isEmpty());
        assertFalse(StructureCollisionRegistry.snapshot().contains(removed));
        assertTrue(StructureCollisionRegistry.snapshot().contains(elsewhere));
    }

    @Test
    void aStructureWiderThanTheIndexIsStillFoundAnywhereAlongIt() {
        Hull probe = hull(level, CRATE, -1000, -1000);
        Hull wall = hull(level, new CollisionShape(Vec3.ZERO,
                List.of(CollisionPart.axisAligned(new AABB(-400, 0, -1, 400, 1, 1)))), 0, 0);
        assertEquals(List.of(wall), StructureCollisionRegistry.structuresNear(probe, around(-350, 0), .01));
        assertEquals(List.of(wall), StructureCollisionRegistry.structuresNear(probe, around(390, 0), .01));
        assertTrue(StructureCollisionRegistry.structuresNear(probe, around(0, 50), .01).isEmpty());
    }

    private static final class Hull extends Entity implements CollidableStructure {
        private final List<CollisionGroup> groups;

        Hull(Level level, CollisionShape shape) {
            super(EntityType.ARMOR_STAND, level);
            groups = List.of(CollisionGroup.fixed("solid", shape));
        }

        @Override
        public List<CollisionGroup> collisionGroups() {
            return groups;
        }

        @Override
        public StructureTransform collisionTransform() {
            return new StructureTransform(getX(), getY(), getZ(), getYRot());
        }

        @Override
        public StructureTransform previousCollisionTransform() {
            return collisionTransform();
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {}

        @Override
        protected void readAdditionalSaveData(CompoundTag tag) {}

        @Override
        protected void addAdditionalSaveData(CompoundTag tag) {}
    }
}
