package me.mss1r.axiomata.ballistics;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProjectilePassageTest {
    @Test
    void tracesTheMovementThroughAConsumerDefinedOpening() {
        Vec3 start = new Vec3(2, 3, 4);
        Vec3 movement = new Vec3(0, 0, 2);
        ProjectilePassThroughControl opening = (from, to) -> {
            assertEquals(start, from);
            assertEquals(start.add(movement), to);
            return true;
        };
        assertTrue(ProjectilePassageHandler.allowsPassage(opening, start, movement, new Vec3(2, 3, 5)));
    }

    @Test
    void stationaryProjectileUsesTheImpactPoint() {
        Vec3 hit = new Vec3(1, 0, 0);
        ProjectilePassThroughControl opening = (from, to) -> to.equals(hit);
        assertTrue(ProjectilePassageHandler.allowsPassage(opening, Vec3.ZERO, Vec3.ZERO, hit));
    }

    @Test
    void closedOpeningDoesNotCancelTheImpact() {
        assertFalse(ProjectilePassageHandler.allowsPassage((from, to) -> false,
                Vec3.ZERO, new Vec3(0, 0, 1), new Vec3(0, 0, .5)));
    }
}
