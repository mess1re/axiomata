package me.mss1r.axiomata.collision;

import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.function.Function;

/** Per-query immutable broadphase. Results retain source order for deterministic narrowphase. */
final class CollisionBoxIndex<T> {
    private record Entry<T>(int order, T value, AABB bounds) {}

    private static final class Node<T> {
        AABB bounds;
        Node<T> left, right;
        List<Entry<T>> entries;
    }

    private final Node<T> root;

    CollisionBoxIndex(List<T> values, Function<T, AABB> bounds) {
        List<Entry<T>> entries = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++)
            entries.add(new Entry<>(i, values.get(i), bounds.apply(values.get(i))));
        root = build(entries);
    }

    List<T> query(AABB search) {
        if (root == null || !root.bounds.intersects(search)) return List.of();
        List<Entry<T>> result = new ArrayList<>();
        collect(root, search, result);
        if (result.isEmpty()) return List.of();
        result.sort(Comparator.comparingInt(Entry::order));
        return result.stream().map(Entry::value).toList();
    }

    boolean any(AABB search) {
        return any(root, search);
    }

    private static boolean any(Node<?> node, AABB search) {
        if (node == null || !node.bounds.intersects(search)) return false;
        if (node.entries != null) {
            for (var entry : node.entries) if (entry.bounds.intersects(search)) return true;
            return false;
        }
        return any(node.left, search) || any(node.right, search);
    }

    private static <T> Node<T> build(List<Entry<T>> entries) {
        if (entries.isEmpty()) return null;
        Node<T> node = new Node<>();
        node.bounds = entries.get(0).bounds;
        for (var entry : entries) node.bounds = node.bounds.minmax(entry.bounds);
        if (entries.size() <= 8) {
            node.entries = List.copyOf(entries);
            return node;
        }
        int axis =
                node.bounds.getXsize() >= node.bounds.getYsize()
                                && node.bounds.getXsize() >= node.bounds.getZsize()
                        ? 0
                        : node.bounds.getYsize() >= node.bounds.getZsize() ? 1 : 2;
        entries.sort(
                Comparator.comparingDouble(
                        e ->
                                axis == 0
                                        ? e.bounds.minX + e.bounds.maxX
                                        : axis == 1
                                                ? e.bounds.minY + e.bounds.maxY
                                                : e.bounds.minZ + e.bounds.maxZ));
        int middle = entries.size() / 2;
        node.left = build(new ArrayList<>(entries.subList(0, middle)));
        node.right = build(new ArrayList<>(entries.subList(middle, entries.size())));
        return node;
    }

    private static <T> void collect(Node<T> node, AABB search, List<Entry<T>> result) {
        if (node == null || !node.bounds.intersects(search)) return;
        if (node.entries != null) {
            for (var entry : node.entries) if (entry.bounds.intersects(search)) result.add(entry);
        } else {
            collect(node.left, search, result);
            collect(node.right, search, result);
        }
    }
}
