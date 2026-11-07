package me.mss1r.axiomata.ballistics.impact;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FragmentSampleTest {
    @Test void samplingDoesNotChangeSizePenetrationOrExpectedDamage() {
        var cloud = new Fragmentation.Cloud(1000, .002, .01, 100, 10000, 0, 0);
        var scale = new DamageScale(400, 20, .5);
        var material = new Penetration.Material(300000, 1000);
        double expected = 1000 * scale.damage(3);
        for (int traces : new int[]{1, 8, 32, 100, 1000}) {
            var sample = FragmentSample.of(cloud, traces);
            assertEquals(.002, sample.massKg());
            assertEquals(.01, sample.diameterM());
            assertEquals(expected, traces * sample.damage(scale, 3), 1e-8);
            var passage = Penetration.through(sample.massKg(), Math.PI * sample.diameterM() * sample.diameterM() / 4,
                    100, .5, material);
            var reference = Penetration.through(.002, Math.PI * .01 * .01 / 4, 100, .5, material);
            assertEquals(reference, passage);
        }
    }

    @Test void nonlinearDamageIsEvaluatedBeforeSummingHits() {
        var scale = new DamageScale(400, 20, .5);
        var sample = new FragmentSample(.002, .01, 10);
        double separate = sample.damage(scale, 4) + sample.damage(scale, 9);
        assertEquals(10 * (scale.damage(4) + scale.damage(9)), separate, 1e-10);
        assertNotEquals(scale.damage(10 * (4 + 9)), separate);
    }
}
