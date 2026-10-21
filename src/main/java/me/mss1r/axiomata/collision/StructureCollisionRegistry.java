package me.mss1r.axiomata.collision;

import com.google.common.collect.MapMaker;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Live structures and their world bounds, indexed by chunk column per level. A query only looks
 * at the columns its box touches, so a crowded server does not scan every structure per query.
 */
public final class StructureCollisionRegistry {
    /** Bounds spanning more columns than this are checked by every query in their level instead. */
    private static final int MAX_COLUMNS = 64;
    /** Removed entities are dropped when a query meets them; this sweep catches the rest. */
    private static final int SWEEP_QUERIES = 256;

    private static final Object LOCK = new Object();
    private static final Map<CollidableStructure, Entry> STRUCTURES = new LinkedHashMap<>();
    private static final Map<Level, Index> INDEX = new MapMaker().weakKeys().makeMap();
    private static final List<Entry> DIRTY = new ArrayList<>();
    private static long order;
    private static int queries;

    private static final class Entry {
        final CollidableStructure structure;
        final long order;
        AABB bounds;
        boolean dirty;
        Index index;
        long[] columns = new long[0];
        boolean wide;

        Entry(CollidableStructure structure, long order) {
            this.structure = structure;
            this.order = order;
        }
    }

    private static final class Index {
        final Long2ObjectOpenHashMap<List<Entry>> columns = new Long2ObjectOpenHashMap<>();
        final List<Entry> wide = new ArrayList<>();
    }

    private StructureCollisionRegistry() {
    }

    public static void register(CollidableStructure structure) {
        synchronized (LOCK) {
            Entry entry = STRUCTURES.computeIfAbsent(structure, key -> new Entry(key, order++));
            entry.bounds = null;
            if (!entry.dirty) {
                entry.dirty = true;
                DIRTY.add(entry);
            }
        }
    }

    public static void unregister(CollidableStructure structure) {
        synchronized (LOCK) {
            Entry entry = STRUCTURES.remove(structure);
            if (entry != null) unindex(entry);
        }
    }

    public static AABB worldBounds(CollidableStructure structure) {
        synchronized (LOCK) {
            Entry entry = STRUCTURES.get(structure);
            if (entry != null && entry.bounds != null) {
                return entry.bounds;
            }
        }

        AABB bounds = StructureCollisionResolver.worldBounds(structure);
        synchronized (LOCK) {
            // Bounds may be calculated off-thread while the structure is removed. Do not put a
            // dead entry back into the cache after that race.
            Entry entry = STRUCTURES.get(structure);
            if (entry != null) {
                entry.bounds = bounds;
            }
        }
        return bounds;
    }

    public static List<CollidableStructure> snapshot() {
        // Async pathfinding uses snapshots. It must not populate bounds from a half-updated
        // structure while the server thread is moving it.
        synchronized (LOCK) {
            return new ArrayList<>(STRUCTURES.keySet());
        }
    }

    /** Structures whose bounds reach the path, in registration order. */
    public static List<CollidableStructure> structuresNear(Entity movingEntity, AABB path, double margin) {
        reindexMoved();
        AABB reach = path.inflate(margin);
        List<Entry> found = new ArrayList<>();
        synchronized (LOCK) {
            if (++queries >= SWEEP_QUERIES) {
                queries = 0;
                sweepRemoved();
            }
            Index index = INDEX.get(movingEntity.level());
            if (index == null) return new ArrayList<>();
            Set<Entry> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            for (int x = column(reach.minX); x <= column(reach.maxX); x++) {
                for (int z = column(reach.minZ); z <= column(reach.maxZ); z++) {
                    List<Entry> column = index.columns.get(ChunkPos.asLong(x, z));
                    if (column != null) {
                        for (Entry entry : column) if (seen.add(entry)) found.add(entry);
                    }
                }
            }
            found.addAll(index.wide);
        }

        List<CollidableStructure> structures = new ArrayList<>(found.size());
        found.sort(Comparator.comparingLong(entry -> entry.order));
        for (Entry entry : found) {
            CollidableStructure structure = entry.structure;
            Entity structureEntity = (Entity) structure;
            if (structureEntity.isRemoved()) {
                unregister(structure);
                continue;
            }
            if (structureEntity == movingEntity
                    || structureEntity.level() != movingEntity.level()
                    || movingEntity.isPassengerOfSameVehicle(structureEntity)
                    || !intersects(worldBounds(structure), path, margin)) {
                continue;
            }
            structures.add(structure);
        }
        return structures;
    }

    /** Moves every structure registered since the last query to the columns of its new bounds. */
    private static void reindexMoved() {
        List<Entry> moved;
        synchronized (LOCK) {
            if (DIRTY.isEmpty()) return;
            moved = new ArrayList<>(DIRTY);
            DIRTY.clear();
            for (Entry entry : moved) entry.dirty = false;
        }
        for (Entry entry : moved) {
            if (!(entry.structure instanceof Entity entity)) continue;
            AABB bounds = worldBounds(entry.structure);
            synchronized (LOCK) {
                if (STRUCTURES.get(entry.structure) != entry) continue;
                unindex(entry);
                if (entity.isRemoved()) STRUCTURES.remove(entry.structure);
                else if (bounds != null) index(entry, entity.level(), bounds);
            }
        }
    }

    private static void index(Entry entry, Level level, AABB bounds) {
        Index index = INDEX.computeIfAbsent(level, key -> new Index());
        entry.index = index;
        int minX = column(bounds.minX), maxX = column(bounds.maxX);
        int minZ = column(bounds.minZ), maxZ = column(bounds.maxZ);
        long count = (long) (maxX - minX + 1) * (maxZ - minZ + 1);
        if (count > MAX_COLUMNS) {
            entry.wide = true;
            index.wide.add(entry);
            return;
        }
        entry.columns = new long[(int) count];
        int i = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                long key = ChunkPos.asLong(x, z);
                entry.columns[i++] = key;
                index.columns.computeIfAbsent(key, k -> new ArrayList<>(2)).add(entry);
            }
        }
    }

    private static void unindex(Entry entry) {
        Index index = entry.index;
        if (index == null) return;
        if (entry.wide) {
            index.wide.remove(entry);
        } else {
            for (long key : entry.columns) {
                List<Entry> column = index.columns.get(key);
                if (column == null) continue;
                column.remove(entry);
                if (column.isEmpty()) index.columns.remove(key);
            }
        }
        entry.index = null;
        entry.columns = new long[0];
        entry.wide = false;
    }

    private static void sweepRemoved() {
        Iterator<Entry> iterator = STRUCTURES.values().iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (entry.structure instanceof Entity entity && entity.isRemoved()) {
                iterator.remove();
                unindex(entry);
            }
        }
    }

    private static int column(double coordinate) {
        return (int) Math.floor(coordinate) >> 4;
    }

    private static boolean intersects(AABB first, AABB second, double margin) {
        return first != null && first.inflate(margin).intersects(second);
    }
}
