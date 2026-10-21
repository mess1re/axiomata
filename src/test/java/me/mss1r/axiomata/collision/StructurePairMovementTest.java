package me.mss1r.axiomata.collision;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StructurePairMovementTest {
    private static final StructureTransform ORIGIN = new StructureTransform(0, 0, 0, 0);
    private static final List<CollisionGroup> CARGO = box("cargo", -.5, 0, -.5, .5, 1, .5);
    private static final List<CollisionGroup> DECK = box("deck", -5, -.25, -5, 5, 0, 5);

    private static List<CollisionGroup> box(String name, double x, double y, double z, double x2, double y2, double z2) {
        return List.of(CollisionGroup.fixed(name, new CollisionShape(Vec3.ZERO,
                List.of(CollisionPart.axisAligned(new AABB(x, y, z, x2, y2, z2))))));
    }

    @Test void landsOnActualDeckAndReportsTheSupportingGroup() {
        var response = StructureCollisionResolver.resolveStructureMovement(new StructureTransform(0, 3, 0, 34), CARGO,
                ORIGIN, DECK, new Vec3(0, -10, 0));
        assertEquals(-3, response.allowed().y, .001);
        assertEquals("deck", response.groundGroup().name());
    }

    @Test void continuousSweepStopsAtAThinWall() {
        var wall = box("wall", 0, -2, -2, .01, 2, 2);
        var response = StructureCollisionResolver.resolveStructureMovement(new StructureTransform(-10, 0, 0, 0), CARGO,
                ORIGIN, wall, new Vec3(100, 0, 0));
        assertEquals(9.5, response.allowed().x, .001);
        assertNull(response.groundGroup());
    }

    @Test void slidesAlongWallWithoutCancellingTangentialMotion() {
        var wall = box("wall", 0, -2, -10, .2, 2, 10);
        var response = StructureCollisionResolver.resolveStructureMovement(new StructureTransform(-2, 0, 0, 0), CARGO,
                ORIGIN, wall, new Vec3(5, 0, 2));
        assertEquals(1.5, response.allowed().x, .001);
        assertEquals(2, response.allowed().z, .001);
    }

    @Test void uprightSupportIsNotInferredFromTouchingASide() {
        var wall = box("wall", .5, -5, -5, 1, 5, 5);
        assertNull(StructureCollisionResolver.supportingGroup(ORIGIN, wall, ORIGIN, CARGO, .03));
    }

    @Test void ceilingContactIsNotSupport() {
        var ceiling = box("ceiling", -5, 1, -5, 5, 2, 5);
        assertNull(StructureCollisionResolver.supportingGroup(ORIGIN, ceiling, ORIGIN, CARGO, .03));
    }

    @Test void detectsSupportAcrossContactClearance() {
        assertNotNull(StructureCollisionResolver.supportingGroup(ORIGIN, DECK,
                new StructureTransform(0, .005, 0, 0), CARGO, .03));
        assertNull(StructureCollisionResolver.supportingGroup(ORIGIN, DECK,
                new StructureTransform(0, .1, 0, 0), CARGO, .03));
    }

    @Test void holesInACompoundDeckStayOpen() {
        var ring = List.of(CollisionGroup.fixed("ring", new CollisionShape(Vec3.ZERO, List.of(
                CollisionPart.axisAligned(new AABB(-3, -.25, -3, -1, 0, 3)),
                CollisionPart.axisAligned(new AABB(1, -.25, -3, 3, 0, 3)),
                CollisionPart.axisAligned(new AABB(-1, -.25, -3, 1, 0, -1)),
                CollisionPart.axisAligned(new AABB(-1, -.25, 1, 1, 0, 3))))));
        var response = StructureCollisionResolver.resolveStructureMovement(new StructureTransform(0, 2, 0, 20), CARGO,
                ORIGIN, ring, new Vec3(0, -4, 0));
        assertEquals(-4, response.allowed().y, 1e-7);
        assertNull(response.groundGroup());
    }

    @Test void aCargoOverhangCanLandWhileItsOriginMissesTheDeck() {
        var feet = box("foot", 3, 0, -.5, 4, 1, .5);
        var smallDeck = box("deck", 3, -.25, -1, 5, 0, 1);
        assertNotNull(StructureCollisionResolver.supportingGroup(ORIGIN, smallDeck, ORIGIN, feet, .03));
    }

    @Test void aRotatedBeamDoesNotBecomeItsEnclosingAabb() {
        var beam = box("beam", -5, -.25, -.1, 5, 0, .1);
        assertNull(StructureCollisionResolver.supportingGroup(new StructureTransform(0, 0, 0, 45), beam,
                new StructureTransform(3, 0, -3, 0), CARGO, .03));
    }

    @Test void localGroupAndCubePosesAreBothRespected() {
        var posed = List.of(new CollisionGroup("deck", new CollisionShape(Vec3.ZERO,
                List.of(new CollisionPart(new AABB(-1, -.25, -1, 1, 0, 1), CollisionPose.translated(new Vec3(2, 0, 0))))),
                CollisionPose.aroundY(Vec3.ZERO, (float)(Math.PI / 2))));
        assertNotNull(StructureCollisionResolver.supportingGroup(ORIGIN, posed,
                new StructureTransform(0, 0, -2, 0), CARGO, .03));
        assertNull(StructureCollisionResolver.supportingGroup(ORIGIN, posed, ORIGIN, CARGO, .03));
    }

    @Test void separatesAnEmbeddedCompoundBodyUpward() {
        var response = StructureCollisionResolver.resolveStructures(ORIGIN, DECK,
                new StructureTransform(0, -.1, 0, 37), CARGO, Vec3.ZERO);
        assertEquals(.1001, response.pushOut().y, .00001);
        assertEquals(0, response.pushOut().horizontalDistance(), 1e-7);
    }

    @Test void ordinaryBoxAndCompoundBoxFollowTheSameSolver() {
        var movement = new Vec3(3, -2, 1);
        var transform = new StructureTransform(0, 1, 0, 0);
        var compound = StructureCollisionResolver.resolveStructureMovement(transform, CARGO, ORIGIN, DECK, movement);
        var ordinary = StructureCollisionResolver.resolveMovement(new TestStructure(ORIGIN, DECK),
                new AABB(-.5, 1, -.5, .5, 2, .5), movement, 0);
        assertEquals(ordinary.allowed(), compound.allowed());
        assertEquals(ordinary.isSupported(), compound.isSupported());
    }

    private record TestStructure(StructureTransform collisionTransform, List<CollisionGroup> collisionGroups) implements CollidableStructure {
        public StructureTransform previousCollisionTransform() { return collisionTransform; }
    }
}
