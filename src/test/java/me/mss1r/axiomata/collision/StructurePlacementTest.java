package me.mss1r.axiomata.collision;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StructurePlacementTest {
    private static final StructureTransform ORIGIN = new StructureTransform(0, 0, 0, 0);
    private static CollisionGroup box(String name, AABB box) {
        return CollisionGroup.fixed(name, new CollisionShape(Vec3.ZERO, List.of(CollisionPart.axisAligned(box))));
    }
    private static final CollisionGroup CARGO = box("cargo", new AABB(-.5, -1, -.5, .5, 1, .5));
    private static final CollisionGroup DECK = box("deck", new AABB(-5, -.25, -5, 5, 0, 5));
    private static final Structure CANDIDATE = new Structure(ORIGIN, List.of(CARGO));

    @Test void alignsTheActualUndersideRatherThanTheEntityOrigin() {
        var result = StructurePlacement.findOnTop(CANDIDATE,
                new Structure(new StructureTransform(10, 3, 10, 20), List.of(DECK)), 10, 10, 45).orElseThrow();
        assertEquals(4, result.transform().y(), .001);
        assertEquals(45, result.transform().yawDegrees(), 1e-6);
        assertEquals("deck", result.support().name());
    }

    @Test void picksTheUpperDeckWhenSeveralLevelsOverlap() {
        var upper = box("upper", new AABB(-5, 3.75, -5, 5, 4, 5));
        var result = StructurePlacement.findOnTop(CANDIDATE, new Structure(ORIGIN, List.of(DECK, upper)), 0, 0, 0).orElseThrow();
        assertEquals(5, result.transform().y(), .001);
        assertEquals("upper", result.support().name());
    }

    @Test void doesNotPlaceIntoADeckOpening() {
        var sides = List.of(box("left", new AABB(-5, -.25, -5, -1, 0, 5)),
                box("right", new AABB(1, -.25, -5, 5, 0, 5)));
        assertTrue(StructurePlacement.findOnTop(CANDIDATE, new Structure(ORIGIN, sides), 0, 0, 0).isEmpty());
    }

    @Test void ignoresGeometryOnlyUsedForPicking() {
        var upper = box("decoration", new AABB(-5, 4, -5, 5, 5, 5));
        CollidableStructure support = new CollidableStructure() {
            public List<CollisionGroup> collisionGroups() { return List.of(DECK, upper); }
            public List<CollisionGroup> solidCollisionGroups() { return List.of(DECK); }
            public StructureTransform collisionTransform() { return ORIGIN; }
            public StructureTransform previousCollisionTransform() { return ORIGIN; }
        };
        assertEquals(1, StructurePlacement.findOnTop(CANDIDATE, support, 0, 0, 0).orElseThrow().transform().y(), .001);
    }

    @Test void rejectsEmptyGeometryAndNonFiniteInputs() {
        var support = new Structure(ORIGIN, List.of(DECK));
        assertTrue(StructurePlacement.findOnTop(new Structure(ORIGIN, List.of()), support, 0, 0, 0).isEmpty());
        assertTrue(StructurePlacement.findOnTop(CANDIDATE, support, Double.NaN, 0, 0).isEmpty());
        assertTrue(StructurePlacement.findOnTop(CANDIDATE, support, 0, 0, Float.POSITIVE_INFINITY).isEmpty());
        assertTrue(StructurePlacement.findOnTop(support, support, 0, 0, 0).isEmpty());
    }

    private record Structure(StructureTransform collisionTransform, List<CollisionGroup> collisionGroups) implements CollidableStructure {
        public StructureTransform previousCollisionTransform() { return collisionTransform; }
    }
}
