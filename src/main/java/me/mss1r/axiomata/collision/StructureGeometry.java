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

    public static List<ClimbableGroup> climbables(CollidableStructure structure) {
        return unfinished(structure)
                ? ConstructionGeometry.builtClimbables((UnderConstruction) structure, structure.climbableGroups())
                : structure.climbableGroups();
    }

    private static boolean unfinished(CollidableStructure structure) {
        return structure instanceof UnderConstruction machine && !machine.isFullyBuilt();
    }
}
