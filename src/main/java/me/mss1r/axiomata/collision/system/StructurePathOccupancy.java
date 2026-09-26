package me.mss1r.axiomata.collision.system;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPart;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.StructureTransform;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

// Path searches ask about far too many nodes for per-node SAT. Solid model geometry is rasterized
// into block coordinates and kept until the structure changes placement. Touching a block corner
// claims the whole block on purpose: pathfinding should give structures a little clearance.
final class StructurePathOccupancy {
    private static final double POSITION_STEP = 0.25D;
    private static final double YAW_STEP = 5.0D;

    private static final int MAX_BLOCKS = 40000;

    private static final Map<CollidableStructure, StructurePathOccupancy> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final long placement;
    private final LongOpenHashSet blocks;
    private final AABB bounds;

    private StructurePathOccupancy(long placement, LongOpenHashSet blocks, AABB bounds) {
        this.placement = placement;
        this.blocks = blocks;
        this.bounds = bounds;
    }
    static AABB bounds(CollidableStructure structure) {
        StructurePathOccupancy occupancy = of(structure);
        return occupancy == null ? null : occupancy.bounds;
    }

    static boolean occupies(CollidableStructure structure, int x, int y, int z) {
        StructurePathOccupancy occupancy = of(structure);
        return occupancy != null && occupancy.blocks.contains(BlockPos.asLong(x, y, z));
    }

    private static StructurePathOccupancy of(CollidableStructure structure) {
        StructureTransform transform = structure.collisionTransform();
        long placement = placementKey(transform);

        StructurePathOccupancy cached = CACHE.get(structure);
        if (cached != null && cached.placement == placement) {
            return cached;
        }

        List<CollisionGroup> groups = structure.solidCollisionGroups();
        LongOpenHashSet blocks = mark(transform, groups);
        StructurePathOccupancy occupancy = new StructurePathOccupancy(placement, blocks,
                StructureCollisionResolver.worldBounds(transform, groups));
        CACHE.put(structure, occupancy);
        return occupancy;
    }

    private static LongOpenHashSet mark(StructureTransform transform, List<CollisionGroup> groups) {
        LongOpenHashSet blocks = new LongOpenHashSet();
        for (CollisionGroup group : groups) {
            for (CollisionPart part : group.parts()) {
                AABB world = worldExtent(transform, group, part);
                for (int x = Mth.floor(world.minX); x <= Mth.floor(world.maxX); x++) {
                    for (int y = Mth.floor(world.minY); y <= Mth.floor(world.maxY); y++) {
                        for (int z = Mth.floor(world.minZ); z <= Mth.floor(world.maxZ); z++) {
                            if (blocks.size() >= MAX_BLOCKS) {
                                return blocks;
                            }
                            blocks.add(BlockPos.asLong(x, y, z));
                        }
                    }
                }
            }
        }
        return blocks;
    }

    private static AABB worldExtent(StructureTransform transform, CollisionGroup group, CollisionPart part) {
        AABB box = part.box();
        AABB extent = null;
        for (double x : new double[]{box.minX, box.maxX}) {
            for (double y : new double[]{box.minY, box.maxY}) {
                for (double z : new double[]{box.minZ, box.maxZ}) {
                    Vec3 corner = transform.toWorld(group.fromPart(part, new Vec3(x, y, z)));
                    AABB point = new AABB(corner, corner);
                    extent = extent == null ? point : extent.minmax(point);
                }
            }
        }
        return extent == null ? new AABB(Vec3.ZERO, Vec3.ZERO) : extent;
    }
    private static long placementKey(StructureTransform transform) {
        // A slowly moving machine does not need a full raster rebuild every tick.
        long x = Math.round(transform.x() / POSITION_STEP);
        long y = Math.round(transform.y() / POSITION_STEP);
        long z = Math.round(transform.z() / POSITION_STEP);
        long yaw = Math.round(Mth.wrapDegrees(transform.yawDegrees()) / YAW_STEP);
        return ((x * 31L + y) * 31L + z) * 31L + yaw;
    }
}
