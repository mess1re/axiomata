package me.mss1r.axiomata.collision.system;

import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.StructureCollisionRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class StructurePathfindingSystem {
    private static final double GATHER_RADIUS = 48.0D;

    private static final double SELF_MARGIN = 0.5D;

    private static final boolean ENABLED =
            !"off".equalsIgnoreCase(System.getProperty("axiomata.structurePathfinding", "on"));

    private static final Map<Mob, Gathered> NEARBY =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());

    private StructurePathfindingSystem() {
    }
    public static boolean blocked(Mob mob, int x, int y, int z) {
        if (!ENABLED) {
            return false;
        }

        List<CollidableStructure> structures = gather(mob);
        if (structures.isEmpty()) {
            return false;
        }

        // Never invalidate the start node. A mob pressed against a structure may already stand in
        // a conservatively rasterized block; rejecting it makes the next search jump above/below.
        if (occupiedByTheMobItself(mob, x, y, z)) {
            return false;
        }

        for (CollidableStructure structure : structures) {
            AABB bounds = StructurePathOccupancy.bounds(structure);
            if (bounds == null || !bounds.intersects(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D)) {
                continue;
            }
            if (StructurePathOccupancy.occupies(structure, x, y, z)) {
                return true;
            }
        }
        return false;
    }
    private static List<CollidableStructure> gather(Mob mob) {
        if (mob.level().isClientSide()) {
            return List.of();
        }

        long now = mob.level().getGameTime();
        Gathered cached = NEARBY.get(mob);
        if (cached != null && cached.tick == now) {
            return cached.structures;
        }

        // This can run on a path worker. Use the registry snapshot and compute occupancy locally
        // instead of touching the server thread's per-tick bounds cache.
        AABB reach = mob.getBoundingBox().inflate(GATHER_RADIUS);
        List<CollidableStructure> structures = new java.util.ArrayList<>();
        for (CollidableStructure candidate : StructureCollisionRegistry.snapshot()) {
            if (!(candidate instanceof Entity entity) || entity.isRemoved()
                    || entity.level() != mob.level() || entity == mob || rides(mob, entity)) {
                continue;
            }
            AABB bounds = StructurePathOccupancy.bounds(candidate);
            if (bounds != null && bounds.intersects(reach)) {
                structures.add(candidate);
            }
        }

        NEARBY.put(mob, new Gathered(now, structures));
        return structures;
    }

    private static boolean occupiedByTheMobItself(Mob mob, int x, int y, int z) {
        AABB standing = mob.getBoundingBox().inflate(SELF_MARGIN, 0.0D, SELF_MARGIN)
                .expandTowards(0.0D, -1.0D, 0.0D);
        return standing.intersects(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D);
    }

    private static boolean rides(Mob mob, Entity structure) {
        for (Entity vehicle = mob.getVehicle(); vehicle != null; vehicle = vehicle.getVehicle()) {
            if (vehicle == structure) {
                return true;
            }
        }
        return false;
    }

    private record Gathered(long tick, List<CollidableStructure> structures) {
    }
}
