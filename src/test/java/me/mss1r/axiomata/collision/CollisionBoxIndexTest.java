package me.mss1r.axiomata.collision;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.world.phys.AABB;

import org.junit.jupiter.api.Test;

import java.util.*;

class CollisionBoxIndexTest {
    @Test
    void matchesBruteForceAndRetainsOriginalOrder() {
        var random = new Random(413);
        List<AABB> boxes = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            double x = random.nextDouble() * 100,
                    y = random.nextDouble() * 20,
                    z = random.nextDouble() * 100;
            boxes.add(new AABB(x, y, z, x + 1 + random.nextDouble() * 5, y + 1, z + 2));
        }
        var index = new CollisionBoxIndex<>(boxes, box -> box);
        for (int i = 0; i < 500; i++) {
            double x = random.nextDouble() * 100, z = random.nextDouble() * 100;
            AABB search = new AABB(x, 0, z, x + 4, 30, z + 4);
            assertEquals(
                    boxes.stream().filter(b -> b.intersects(search)).toList(), index.query(search));
        }
        assertTrue(
                new CollisionBoxIndex<AABB>(List.of(), b -> b)
                        .query(new AABB(0, 0, 0, 1, 1, 1))
                        .isEmpty());
    }
}
