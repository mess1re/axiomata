package me.mss1r.axiomata.collision;

import me.mss1r.axiomata.blueprint.api.construction.ConstructionGeometry;
import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;

import java.util.List;

/**
 * The geometry Axiomata's systems collide with. A structure still under construction is solid only where it is
 * built and climbable only on built ladders, whatever its finished geometry is.
 */
public final class StructureGeometry {
    private StructureGeometry() {
    }

    public static List<CollisionGroup> solid(CollidableStructure structure) {
        return unfinished(structure) ? ConstructionGeometry.builtGroups((UnderConstruction) structure)
                : structure.solidCollisionGroups();
    }

    public static List<CollisionGroup> previousSolid(CollidableStructure structure) {
        return unfinished(structure) ? ConstructionGeometry.builtGroups((UnderConstruction) structure)
                : structure.previousSolidCollisionGroups();
    }

    /**
     * Contact footprint of a load. An unfinished build reserves its finished footing even before
     * its first section exists. Used for placement and to find what carries it, never as a
     * physical obstacle or as a surface on which another entity can stand.
     */
    public static List<CollisionGroup> supportContact(CollidableStructure structure) {
        List<CollisionGroup> solid = structure.solidCollisionGroups();
        List<CollisionGroup> contact = structure.supportContactGroups();
        if (contact.isEmpty()) return solid;
        var groups = new java.util.ArrayList<>(solid);
        groups.addAll(contact);
        return List.copyOf(groups);
    }

    public static List<ClimbableGroup> climbables(CollidableStructure structure) {
        return unfinished(structure)
                ? ConstructionGeometry.builtClimbables((UnderConstruction) structure, structure.climbableGroups())
                : structure.climbableGroups();
    }

    private static boolean unfinished(CollidableStructure structure) {
        return structure instanceof UnderConstruction machine && !machine.isFullyBuilt();
    }
}
