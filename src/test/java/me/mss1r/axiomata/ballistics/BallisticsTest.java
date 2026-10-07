package me.mss1r.axiomata.ballistics;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BallisticsTest {
    @Test
    void dragPrecedesGravityAndDoesNotReverseVelocity() {
        var flight = new Ballistics.Flight(0.025, 0.01, 0, 0);
        assertEquals(new Vec3(2.91, -0.025, 0), flight.afterMove(new Vec3(3, 0, 0)));
        assertEquals(new Vec3(0, -0.025, 0), flight.afterMove(new Vec3(200, 0, 0)));
    }

    @Test
    void aimingSolutionMatchesDiscreteFlight() {
        Vec3 origin = Vec3.ZERO;
        Vec3 target = new Vec3(100, 0, 0);
        var flight = Ballistics.Flight.ballistic(0.0005);
        double speed = 7;
        float pitch = Ballistics.calculateLowAnglePitch(origin, target, speed, flight);
        assertTrue(Float.isFinite(pitch));
        double elevation = Math.toRadians(-pitch);
        Vec3 velocity = new Vec3(speed * Math.cos(elevation), speed * Math.sin(elevation), 0);
        Vec3 at = origin;
        for (int i = 0; i < 100; i++) {
            Vec3 next = at.add(velocity);
            if (next.x >= target.x) {
                double fraction = (target.x - at.x) / (next.x - at.x);
                assertEquals(target.y, at.y + (next.y - at.y) * fraction, 0.001);
                return;
            }
            at = next;
            velocity = flight.afterMove(velocity);
        }
        fail("Did not reach the target plane");
    }

    @Test
    void rejectsUnreachableShots() {
        assertTrue(Float.isNaN(Ballistics.calculateLowAnglePitch(Vec3.ZERO, new Vec3(5000, 0, 0), 1,
                Ballistics.GRAVITY)));
        assertTrue(Double.isNaN(Ballistics.calculateFixedArcPower(Vec3.ZERO, new Vec3(100, 100, 0),
                4, 0.1, Ballistics.GRAVITY)));
    }

    @Test
    void penetrationSpendsEnergyAndComposesAcrossLayers() {
        var material = new ImpactResults.Material(300_000, 1000, 1000, 1, 1);
        double first = ImpactResolver.speedThrough(material, 2, 0.025, 140, 0.4);
        double second = ImpactResolver.speedThrough(material, 2, 0.025, first, 0.4);
        double whole = ImpactResolver.speedThrough(material, 2, 0.025, 140, 0.8);
        assertTrue(first > second && second > 0 && first < 140);
        assertEquals(whole, second, 1e-10);
        assertEquals(0, ImpactResolver.speedThrough(material, 2, 0.025, 5, 10));
    }

    @Test
    void penetrationWithoutDragUsesTheStrengthLimit() {
        var material = new ImpactResults.Material(1000, 0, 1000, 1, 1);
        double expected = Math.sqrt(10000 - 2 * 1000 * Math.PI * 0.01 / 4 * 2 / 5);
        assertEquals(expected, ImpactResolver.speedThrough(material, 5, 0.1, 100, 2), 1e-10);
    }
}
