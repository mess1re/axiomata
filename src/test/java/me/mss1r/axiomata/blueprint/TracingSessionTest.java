package me.mss1r.axiomata.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import me.mss1r.axiomata.blueprint.tracing.OutlineField;
import me.mss1r.axiomata.blueprint.tracing.OutlineMask;
import me.mss1r.axiomata.blueprint.tracing.TracingRules;
import me.mss1r.axiomata.blueprint.tracing.TracingSession;
import org.junit.jupiter.api.Test;

class TracingSessionTest {
    private static final int RESOLUTION = 128;

    private static TracingSession session(boolean[][] ink) {
        byte[] bits = new byte[RESOLUTION * RESOLUTION / 8];
        for (int y = 0; y < RESOLUTION; y++) {
            for (int x = 0; x < RESOLUTION; x++) {
                if (ink[y][x]) {
                    int index = y * RESOLUTION + x;
                    bits[index >> 3] |= (byte) (0x80 >>> (index & 7));
                }
            }
        }
        OutlineMask mask = new OutlineMask(RESOLUTION, bits);
        return new TracingSession(mask, new OutlineField(mask), TracingRules.TOLERANCE_PIXELS);
    }

    /** A stroke along cells {@code from..to} of row {@code y}, at the cells' centres on the canvas. */
    private static void stroke(TracingSession session, int y, int from, int to) {
        for (int x = from; x <= to; x++) {
            session.apply(x * 2 + 1, y * 2 + 1);
        }
        session.lift();
    }

    @Test
    void theBrushInksTheCellsAroundTheNibCornersIncluded() {
        boolean[][] ink = new boolean[RESOLUTION][RESOLUTION];
        for (int y = 40; y < 45; y++) {
            for (int x = 40; x < 45; x++) {
                ink[y][x] = true;
            }
        }
        TracingSession session = session(ink);
        session.apply(42 * 2 + 1, 42 * 2 + 1);
        assertTrue(session.covered(41, 41) && session.covered(43, 43), "A three-pixel nib leaves its corners dry");
        assertFalse(session.covered(40, 42), "The nib reaches two cells away");
    }

    @Test
    void liftingThePenFillsATinyGapButNotAMissedStretch() {
        boolean[][] ink = new boolean[RESOLUTION][RESOLUTION];
        for (int x = 10; x <= 70; x++) {
            ink[20][x] = true;
        }
        TracingSession session = session(ink);
        stroke(session, 20, 10, 20);
        stroke(session, 20, 25, 40);
        assertTrue(session.covered(22, 20) && session.covered(23, 20), "A two-cell gap between strokes stayed dry");
        stroke(session, 20, 50, 70);
        assertFalse(session.covered(45, 20), "Eight missed cells were filled for free");
    }

    @Test
    void aMarkNobodyTouchedStaysDry() {
        boolean[][] ink = new boolean[RESOLUTION][RESOLUTION];
        for (int x = 10; x <= 30; x++) {
            ink[20][x] = true;
        }
        ink[60][60] = ink[60][61] = ink[61][60] = true;
        TracingSession session = session(ink);
        stroke(session, 20, 10, 30);
        assertFalse(session.covered(60, 60), "A separate mark was inked without being drawn");
        assertEquals(21.0F / 24.0F, session.coverage(), 1.0E-6F);
    }
}
