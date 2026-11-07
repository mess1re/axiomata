package me.mss1r.axiomata.ballistics.impact;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DirectDamageTest {
    @Test void referenceDamageIsLiteralHp() {
        var damage = new DirectDamage(120, 120);
        assertEquals(120, damage.atSpeed(120));
        assertEquals(97.2, damage.atSpeed(108), 1e-10);
        assertEquals(30, damage.atSpeed(60));
        assertEquals(0, damage.atSpeed(0));
        assertEquals(480, damage.atSpeed(240));
    }

    @Test void obstaclesDoNotChangeTheCalibration() {
        var damage = new DirectDamage(120, 120);
        double afterFirst = 120 * .8;
        double afterSecond = afterFirst * .5;
        assertEquals(19.2, damage.atSpeed(afterSecond), 1e-10);
        assertEquals(120, damage.damageHp());
    }

    @Test void rejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> new DirectDamage(-1, 120));
        assertThrows(IllegalArgumentException.class, () -> new DirectDamage(120, 0));
        assertThrows(IllegalArgumentException.class, () -> new DirectDamage(120, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new DirectDamage(120, 120).atSpeed(-1));
    }
}
