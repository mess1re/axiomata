package me.mss1r.axiomata.structure;

import me.mss1r.axiomata.geometry.LocalBox;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
