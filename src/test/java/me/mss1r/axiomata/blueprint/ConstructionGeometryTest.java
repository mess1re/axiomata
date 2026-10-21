package me.mss1r.axiomata.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import me.mss1r.axiomata.blueprint.api.construction.ConstructionGeometry;
import me.mss1r.axiomata.blueprint.api.visual.BlueprintConstructionVisuals;
import me.mss1r.axiomata.collision.OrientedBox;
import me.mss1r.axiomata.collision.Rotation3;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.StructureTransform;
import me.mss1r.axiomata.geometry.LocalBox;
import me.mss1r.axiomata.structure.SectionBounds;
import me.mss1r.axiomata.structure.StructureSections;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

class ConstructionGeometryTest {
    private static final LocalBox ANY = new LocalBox(0, 0, 0, 1, 1, 1);
    // A floor one block up and a mast standing on it.
    private static final SectionBounds BOUNDS = new SectionBounds(List.of(
            new SectionBounds.Section("floor", ANY,
                    List.of(new OrientedBox(new Vec3(0, 1, 0), new Vec3(2, .1, 2), Rotation3.IDENTITY))),
            new SectionBounds.Section("mast", ANY,
                    List.of(new OrientedBox(new Vec3(0, 4, 0), new Vec3(.2, 3, .2), Rotation3.IDENTITY)))));
    private static final StructureSections SECTIONS = new StructureSections(Map.of("floor", 1, "mast", 1),
            List.of(new StructureSections.Section("floor", Map.of("floor", List.of(0))),
                    new StructureSections.Section("mast", Map.of("mast", List.of(0), "rope_top", List.of()))));

    @Test
    void onlyTheBuiltSectionsAreSolidWhereTheMarkupPlacesThem() {
        var groups = ConstructionGeometry.groups(BOUNDS, Set.of("floor"));
        assertEquals(List.of("floor"), groups.stream().map(group -> group.name()).toList());
        AABB bounds = StructureCollisionResolver.worldBounds(new StructureTransform(10, 64, 10, 0), groups);
        assertEquals(new AABB(8, 64.9, 8, 12, 65.1, 12).toString(), round(bounds).toString());
        assertTrue(ConstructionGeometry.groups(BOUNDS, Set.of()).isEmpty());
    }

    @Test
    void anAnchorAppearsWithItsSectionAndUnlistedBonesWithTheFinishedResult() {
        var floorBuilt = new BlueprintConstructionVisuals.State(Set.of("floor"), "mast", 1, 2);
        assertTrue(ConstructionGeometry.isBoneBuilt(SECTIONS, floorBuilt, "floor"));
        assertFalse(ConstructionGeometry.isBoneBuilt(SECTIONS, floorBuilt, "rope_top"));
        assertFalse(ConstructionGeometry.isBoneBuilt(SECTIONS, floorBuilt, "banner"));
        var finished = new BlueprintConstructionVisuals.State(Set.of("floor", "mast"), "", 2, 2);
        assertTrue(ConstructionGeometry.isBoneBuilt(SECTIONS, finished, "rope_top"));
        assertTrue(ConstructionGeometry.isBoneBuilt(SECTIONS, finished, "banner"));
    }

    private static AABB round(AABB box) {
        return new AABB(r(box.minX), r(box.minY), r(box.minZ), r(box.maxX), r(box.maxY), r(box.maxZ));
    }

    private static double r(double value) {
        return Math.round(value * 1000) / 1000.0;
    }
}
