package me.mss1r.axiomata.collision;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScalarAnimationCurveTest {
    @Test
    void linearTrackPreservesEndpointsAndMidpoint() {
        ScalarAnimationCurve curve = ScalarAnimationCurve.of(
                ScalarAnimationCurve.key(0.0F, -5.0D, ScalarAnimationCurve.Interpolation.LINEAR),
                ScalarAnimationCurve.key(10.0F, 15.0D, ScalarAnimationCurve.Interpolation.LINEAR));

        assertEquals(-5.0D, curve.sample(-1.0F));
        assertEquals(5.0D, curve.sample(5.0F));
        assertEquals(15.0D, curve.sample(20.0F));
    }
}
