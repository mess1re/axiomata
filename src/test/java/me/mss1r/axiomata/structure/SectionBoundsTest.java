package me.mss1r.axiomata.structure;

import me.mss1r.axiomata.geometry.LocalBox;
import me.mss1r.axiomata.collision.OrientedBox;
import me.mss1r.axiomata.collision.Rotation3;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SectionBoundsTest {
    @Test
    void findsAndTestsNamedLocalBounds() {
        SectionBounds bounds = new SectionBounds(List.of(
                new SectionBounds.Section("frame", new LocalBox(-1, 0, -2, 1, 2, 2))
        ));

        LocalBox frame = bounds.find("frame").orElseThrow().bounds();
        assertTrue(frame.contains(0, 1, 0));
        assertTrue(frame.contains(1.1, 1, 0, 0.1));
        assertFalse(frame.contains(1.2, 1, 0, 0.1));
    }

    @Test
    void missesTheGapBetweenCubes() {
        var section = section("frame", box(-1, 0, 0), box(1, 0, 0));
        assertFalse(section.clip(new Vec3(0, 0, -2), new Vec3(0, 0, 2), 0.5).isPresent());
        assertTrue(section.clip(new Vec3(1, 0, -2), new Vec3(1, 0, 2), 0.5).isPresent());
    }

    @Test
    void followsCubeRotationRatherThanItsEnvelope() {
        var cube = new OrientedBox(Vec3.ZERO, new Vec3(1, 0.1, 0.1), Rotation3.aroundY((float) (Math.PI / 4)));
        var section = section("arm", cube);
        assertTrue(section.clip(new Vec3(0, -1, 0), new Vec3(0, 1, 0), 0).isPresent());
        assertFalse(section.clip(new Vec3(0.65, -1, 0.65), new Vec3(0.65, 1, 0.65), 0).isPresent());
        assertTrue(section.clip(new Vec3(0.65, -1, -0.65), new Vec3(0.65, 1, -0.65), 0).isPresent());
    }

    @Test
    void builtSectionBlocksAHitOnTheActiveSectionButFutureGhostsDoNot() {
        var bounds = new SectionBounds(List.of(section("active", box(0, 0, 1)),
                section("built", box(0, 0, -1)), section("future", box(0, 0, -2))));
        Vec3 start = new Vec3(0, 0, -4);
        Vec3 end = new Vec3(0, 0, 4);
        assertEquals("built", bounds.firstHit(start, end, Set.of("active", "built"), 0).orElseThrow().name());
        assertEquals("active", bounds.firstHit(start, end, Set.of("active"), 0).orElseThrow().name());
    }

    @Test
    void doesNotExtendReachAndHandlesZeroLengthRays() {
        var section = section("wheel", box(0, 0, 1));
        assertFalse(section.clip(new Vec3(0, 0, -2), Vec3.ZERO, 0).isPresent());
        assertFalse(section.clip(Vec3.ZERO, Vec3.ZERO, 0).isPresent());
        assertEquals(0, section.clip(new Vec3(0, 0, 1), new Vec3(0, 0, 1), 0).orElseThrow());
    }

    @Test
    void acceptsLegacyPixelBounds() {
        var section = new SectionBounds.Section("frame", new LocalBox(-32, 0, -8, -16, 16, 8));
        assertTrue(section.clip(new Vec3(1.5, 0.5, -2), new Vec3(1.5, 0.5, 2), 0).isPresent());
        assertFalse(section.clip(new Vec3(-1.5, 0.5, -2), new Vec3(-1.5, 0.5, 2), 0).isPresent());
    }

    private static SectionBounds.Section section(String name, OrientedBox... parts) {
        return new SectionBounds.Section(name, new LocalBox(-32, -32, -32, 32, 32, 32), List.of(parts));
    }

    private static OrientedBox box(double x, double y, double z) {
        return new OrientedBox(new Vec3(x, y, z), new Vec3(0.25, 0.25, 0.25), Rotation3.IDENTITY);
    }
}
