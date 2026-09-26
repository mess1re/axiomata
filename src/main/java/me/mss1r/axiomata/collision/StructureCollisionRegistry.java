package me.mss1r.axiomata.collision;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StructureCollisionRegistry {
    private static final Set<CollidableStructure> STRUCTURES =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<CollidableStructure, AABB> WORLD_BOUNDS = new IdentityHashMap<>();

    private StructureCollisionRegistry() {
    }

    public static void register(CollidableStructure structure) {
        synchronized (STRUCTURES) {
            STRUCTURES.add(structure);
            WORLD_BOUNDS.remove(structure);
        }
    }

    public static void unregister(CollidableStructure structure) {
        synchronized (STRUCTURES) {
            STRUCTURES.remove(structure);
            WORLD_BOUNDS.remove(structure);
        }
    }

    public static AABB worldBounds(CollidableStructure structure) {
        synchronized (STRUCTURES) {
            AABB cached = WORLD_BOUNDS.get(structure);
            if (cached != null) {
                return cached;
            }
        }

        AABB bounds = StructureCollisionResolver.worldBounds(structure);
        synchronized (STRUCTURES) {
            // Bounds may be calculated off-thread while the structure is removed. Do not put a
            // dead entry back into the cache after that race.
            if (STRUCTURES.contains(structure)) {
                WORLD_BOUNDS.put(structure, bounds);
            }
        }
        return bounds;
    }
    public static List<CollidableStructure> snapshot() {
        // Async pathfinding uses snapshots. It must not populate WORLD_BOUNDS from a half-updated
        // structure while the server thread is moving it.
        synchronized (STRUCTURES) {
            return new ArrayList<>(STRUCTURES);
        }
    }

    public static List<CollidableStructure> structuresNear(Entity movingEntity, AABB path, double margin) {
        List<CollidableStructure> structures;
        synchronized (STRUCTURES) {
            Iterator<CollidableStructure> iterator = STRUCTURES.iterator();
            while (iterator.hasNext()) {
                CollidableStructure structure = iterator.next();
                if (structure instanceof Entity entity && entity.isRemoved()) {
                    iterator.remove();
                    WORLD_BOUNDS.remove(structure);
                }
            }
            structures = new ArrayList<>(STRUCTURES);
        }

        structures.removeIf(structure -> !(structure instanceof Entity structureEntity)
                || structureEntity.isRemoved()
                || structureEntity.level() != movingEntity.level()
                || structureEntity == movingEntity
                || movingEntity.isPassengerOfSameVehicle(structureEntity)
                || !intersects(worldBounds(structure), path, margin));
        return structures;
    }

    private static boolean intersects(AABB first, AABB second, double margin) {
        return first != null && first.inflate(margin).intersects(second);
    }
}
