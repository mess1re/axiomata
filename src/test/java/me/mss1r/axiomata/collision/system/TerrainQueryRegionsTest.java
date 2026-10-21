package me.mss1r.axiomata.collision.system;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import java.util.*;

class TerrainQueryRegionsTest {
    @Test
    void coalescingPreservesExactlyTheOccupiedCellsIncludingNegativeCoordinates() {
        var random = new Random(164);
        for (int trial = 0; trial < 100; trial++) {
            Set<Long> source = new HashSet<>();
            for (int i = 0; i < 150; i++)
                source.add(
                        BlockPos.asLong(
                                random.nextInt(12) - 6,
                                random.nextInt(6) - 3,
                                random.nextInt(12) - 6));
            Set<Long> restored = new HashSet<>();
            for (var box : TerrainQueryRegions.merge(source)) {
                for (int x = (int) box.minX / 4; x < box.maxX / 4; x++)
                    for (int y = (int) box.minY / 4; y < box.maxY / 4; y++)
                        for (int z = (int) box.minZ / 4; z < box.maxZ / 4; z++)
                            assertTrue(
                                    restored.add(BlockPos.asLong(x, y, z)),
                                    "No duplicate scanning regions");
            }
            assertEquals(source, restored, "Never fill gaps or drop cells");
        }
    }

    @Test
    void aSolidGridBecomesOneQuery() {
        Set<Long> cells = new HashSet<>();
        for (int x = 0; x < 10; x++)
            for (int y = 0; y < 3; y++)
                for (int z = 0; z < 4; z++) cells.add(BlockPos.asLong(x, y, z));
        assertEquals(1, TerrainQueryRegions.merge(cells).size());
    }
}
