package me.mss1r.axiomata.collision.system;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.*;

/** Exact cell union: coalescing avoids repeating block-iterator margins without filling holes. */
final class TerrainQueryRegions {
    private TerrainQueryRegions() {}

    static List<AABB> merge(Set<Long> cells) {
        Map<Long, SortedSet<Integer>> rows = new TreeMap<>();
        for (long cell : cells)
            rows.computeIfAbsent(
                            BlockPos.asLong(0, BlockPos.getY(cell), BlockPos.getZ(cell)),
                            key -> new TreeSet<>())
                    .add(BlockPos.getX(cell));
        List<AABB> regions = new ArrayList<>();
        for (var row : rows.entrySet()) {
            int y = BlockPos.getY(row.getKey()) * 4, z = BlockPos.getZ(row.getKey()) * 4;
            int start = Integer.MIN_VALUE, end = start;
            for (int x : row.getValue()) {
                if (start != Integer.MIN_VALUE && x != end + 1) {
                    regions.add(new AABB(start * 4, y, z, (end + 1) * 4, y + 4, z + 4));
                    start = Integer.MIN_VALUE;
                }
                if (start == Integer.MIN_VALUE) start = x;
                end = x;
            }
            if (start != Integer.MIN_VALUE)
                regions.add(new AABB(start * 4, y, z, (end + 1) * 4, y + 4, z + 4));
        }
        regions.sort(
                Comparator.comparingDouble((AABB a) -> a.minX)
                        .thenComparingDouble(a -> a.maxX)
                        .thenComparingDouble(a -> a.minY)
                        .thenComparingDouble(a -> a.maxY)
                        .thenComparingDouble(a -> a.minZ));
        regions = join(regions, false);
        regions.sort(
                Comparator.comparingDouble((AABB a) -> a.minX)
                        .thenComparingDouble(a -> a.maxX)
                        .thenComparingDouble(a -> a.minZ)
                        .thenComparingDouble(a -> a.maxZ)
                        .thenComparingDouble(a -> a.minY));
        return join(regions, true);
    }

    private static List<AABB> join(List<AABB> input, boolean vertical) {
        List<AABB> output = new ArrayList<>();
        for (AABB box : input) {
            if (!output.isEmpty()) {
                AABB last = output.get(output.size() - 1);
                boolean adjacent =
                        last.minX == box.minX
                                && last.maxX == box.maxX
                                && (vertical
                                        ? last.minZ == box.minZ
                                                && last.maxZ == box.maxZ
                                                && last.maxY == box.minY
                                        : last.minY == box.minY
                                                && last.maxY == box.maxY
                                                && last.maxZ == box.minZ);
                if (adjacent) {
                    output.set(output.size() - 1, last.minmax(box));
                    continue;
                }
            }
            output.add(box);
        }
        return output;
    }
}
