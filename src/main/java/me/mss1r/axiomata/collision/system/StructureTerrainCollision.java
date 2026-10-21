package me.mss1r.axiomata.collision.system;

import me.mss1r.axiomata.collision.StructureGeometry;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPose;
import me.mss1r.axiomata.collision.StructureCollisionRegistry;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.StructureTransform;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.google.common.collect.MapMaker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class StructureTerrainCollision {
    private static final double MOVEMENT_EPSILON = 1.0E-8D;
    private static final double STEP_MARGIN = 1.0E-4D;
    private static final double MINIMUM_ESCAPE = 1.0E-3D;
    private static final double PENETRATION_EPSILON = 1.0E-8D;
    private static final double MAX_UNSTICK_STEP = 0.15D;

    private StructureTerrainCollision() {}

    /**
     * Full three-axis motion for geometry-backed entities; unlike clampMovement, does not step over
     * terrain.
     */
    public static <T extends Entity & CollidableStructure> Vec3 clampMotion(
            T structure, Vec3 wanted) {
        return clampMotion(structure, wanted, other -> false);
    }

    public static <T extends Entity & CollidableStructure> Vec3 clampMotion(
            T structure, Vec3 wanted, Predicate<CollidableStructure> ignored) {
        return clampMotion(structure, wanted, StructureGeometry.solid(structure), -1.0D, ignored);
    }

    /** Entity.move entry point, retaining the consumer's vanilla step height. */
    public static <T extends Entity & CollidableStructure> Vec3 collideMotion(
            T structure, Vec3 wanted) {
        if (wanted.lengthSqr() <= MOVEMENT_EPSILON * MOVEMENT_EPSILON) return wanted;
        StructureTransform start = structure.collisionTransform();
        if (wanted.y <= 0 && wanted.horizontalDistanceSqr() <= MOVEMENT_EPSILON * MOVEMENT_EPSILON) {
            // Straight down, a load riding on top can never reach terrain before the structure
            // itself does: skip assembling it. Resting stacks settle like this every tick.
            return settle(structure, start, wanted);
        }
        SupportedStructures assembly =
                SupportedStructures.of(structure, StructureGeometry.solid(structure), other -> false);
        List<CollisionGroup> groups = assembly.groups();
        Vec3 allowed = resolveMotion(structure, start, groups, wanted, -1, assembly::contains);
        double height = structure.maxUpStep();
        boolean grounded =
                structure.onGround() || wanted.y < 0 && allowed.y > wanted.y + MOVEMENT_EPSILON;
        if (height <= 0
                || !grounded
                || wanted.y > 0
                || allowed.horizontalDistanceSqr()
                        >= wanted.horizontalDistanceSqr() - MOVEMENT_EPSILON) return allowed;
        Vec3 up =
                resolveMotion(
                        structure, start, groups, new Vec3(0, height, 0), -1, assembly::contains);
        StructureTransform raised = offset(start, up);
        Vec3 across =
                resolveMotion(
                        structure,
                        raised,
                        groups,
                        new Vec3(wanted.x, 0, wanted.z),
                        -1,
                        assembly::contains);
        Vec3 down =
                resolveMotion(
                        structure,
                        offset(raised, across),
                        groups,
                        new Vec3(0, wanted.y - up.y, 0),
                        -1,
                        assembly::contains);
        Vec3 stepped = up.add(across).add(down);
        return stepped.horizontalDistanceSqr() > allowed.horizontalDistanceSqr() + MOVEMENT_EPSILON
                ? stepped
                : allowed;
    }

    /** A resting body is not re-solved while it, its neighbours and its request stay the same. */
    private static final Map<Entity, Rest> RESTING = new MapMaker().weakKeys().makeMap();
    /** Terrain is not watched; a full check this often catches a block removed underneath. */
    private static final int REST_RECHECK_TICKS = 20;

    private record Neighbour(CollidableStructure structure, StructureTransform transform, StructureTransform relative) {
        static Neighbour of(StructureTransform self, CollidableStructure other) {
            StructureTransform transform = other.collisionTransform();
            return new Neighbour(other, transform, StructureTerrainCollision.relative(self, transform));
        }
    }

    private static StructureTransform relative(StructureTransform self, StructureTransform other) {
        Vec3 local = self.toLocal(new Vec3(other.x(), other.y(), other.z()));
        return new StructureTransform(local.x, local.y, local.z,
                net.minecraft.util.Mth.wrapDegrees(other.yawDegrees() - self.yawDegrees()));
    }

    private static boolean near(StructureTransform a, StructureTransform b) {
        return Math.abs(a.x() - b.x()) < 1.0E-6 && Math.abs(a.y() - b.y()) < 1.0E-6
                && Math.abs(a.z() - b.z()) < 1.0E-6
                && Math.abs(net.minecraft.util.Mth.wrapDegrees(a.yawDegrees() - b.yawDegrees())) < 1.0E-4F;
    }

    private record Rest(StructureTransform transform, Vec3 wanted, Vec3 allowed, long since, long recorded,
                        List<Neighbour> neighbours) {
        boolean holds(StructureTransform now, Vec3 request, List<CollidableStructure> nearby, long tick,
                      boolean carried) {
            if (tick - since >= REST_RECHECK_TICKS
                    || request.distanceToSqr(wanted) > 1.0E-12 || nearby.size() != neighbours.size()) return false;
            boolean still = now.equals(transform);
            // A load carried rigidly with its neighbours rests on them exactly as before.
            if (!still && !carried) return false;
            for (int i = 0; i < nearby.size(); i++) {
                Neighbour neighbour = neighbours.get(i);
                if (nearby.get(i) != neighbour.structure()) return false;
                StructureTransform other = nearby.get(i).collisionTransform();
                if (still ? !other.equals(neighbour.transform()) : !near(relative(now, other), neighbour.relative()))
                    return false;
            }
            return true;
        }
    }

    private static <T extends Entity & CollidableStructure> Vec3 settle(
            T structure, StructureTransform start, Vec3 wanted) {
        List<CollisionGroup> groups = StructureGeometry.solid(structure);
        AABB bounds = StructureCollisionResolver.worldBounds(start, groups);
        if (bounds == null) return wanted;
        List<CollidableStructure> nearby =
                StructureCollisionRegistry.structuresNear(structure, bounds.expandTowards(wanted).inflate(0.01D), 0.01D);
        long tick = structure.level().getGameTime();
        Rest rest = RESTING.get(structure);
        if (rest != null
                && rest.holds(start, wanted, nearby, tick, StructureCarrySystem.carriedSince(structure, rest.recorded()))) {
            if (!start.equals(rest.transform())) RESTING.put(structure, rested(structure, start, rest, nearby, rest.since(), tick));
            return rest.allowed();
        }
        Vec3 allowed = resolveMotion(structure, start, groups, wanted, -1, other -> other == structure);
        if (allowed.y > wanted.y + MOVEMENT_EPSILON) {
            // Spread the periodic full checks so a stack settled together does not recheck together.
            long since = rest == null ? tick - Math.floorMod(structure.getId(), REST_RECHECK_TICKS) : tick;
            RESTING.put(structure, rested(structure, start, new Rest(start, wanted, allowed, since, tick, List.of()),
                    nearby, since, tick));
        } else {
            RESTING.remove(structure);
        }
        return allowed;
    }

    private static Rest rested(Entity structure, StructureTransform at, Rest rest, List<CollidableStructure> nearby,
                               long since, long tick) {
        List<Neighbour> neighbours = new ArrayList<>(nearby.size());
        for (CollidableStructure other : nearby) neighbours.add(Neighbour.of(at, other));
        return new Rest(at, rest.wanted(), rest.allowed(), since, tick, neighbours);
    }

    private static StructureTransform offset(StructureTransform transform, Vec3 offset) {
        return new StructureTransform(
                transform.x() + offset.x,
                transform.y() + offset.y,
                transform.z() + offset.z,
                transform.yawDegrees());
    }

    public static <T extends Entity & CollidableStructure> float clampYaw(
            T structure, float wantedYaw) {
        return clampYaw(structure, wantedYaw, other -> false);
    }

    /** Checks intermediate poses too, so a long thin part cannot rotate through an obstacle. */
    public static <T extends Entity & CollidableStructure> float clampYaw(
            T structure, float wantedYaw, Predicate<CollidableStructure> ignored) {
        StructureTransform start = structure.collisionTransform();
        if (!Float.isFinite(wantedYaw)) return start.yawDegrees();
        if (Math.abs(net.minecraft.util.Mth.wrapDegrees(wantedYaw - start.yawDegrees())) < 1.0E-4F) return wantedYaw;
        return clampYaw(structure, wantedYaw,
                SupportedStructures.of(structure, StructureGeometry.solid(structure), ignored), ignored);
    }

    /** As {@link #clampYaw(Entity, float, Predicate)}, with the loads already gathered. */
    static <T extends Entity & CollidableStructure> float clampYaw(
            T structure, float wantedYaw, SupportedStructures assembly, Predicate<CollidableStructure> ignored) {
        StructureTransform start = structure.collisionTransform();
        if (!Float.isFinite(wantedYaw)) return start.yawDegrees();
        float difference = net.minecraft.util.Mth.wrapDegrees(wantedYaw - start.yawDegrees());
        if (Math.abs(difference) < 1.0E-4F) return wantedYaw;
        AABB bounds = StructureCollisionResolver.worldBounds(start, assembly.groups());
        if (bounds == null) return wantedYaw;
        double x = Math.max(Math.abs(bounds.minX - start.x()), Math.abs(bounds.maxX - start.x()));
        double z = Math.max(Math.abs(bounds.minZ - start.z()), Math.abs(bounds.maxZ - start.z()));
        int steps =
                Math.max(
                        1,
                        (int)
                                Math.ceil(
                                        Math.toRadians(Math.abs(difference))
                                                * Math.max(1, Math.hypot(x, z))
                                                / .025D));
        float accepted = start.yawDegrees();
        for (int i = 1; i <= steps; i++) {
            float yaw = start.yawDegrees() + difference * i / steps;
            StructureTransform before =
                    new StructureTransform(start.x(), start.y(), start.z(), accepted);
            StructureTransform next = new StructureTransform(start.x(), start.y(), start.z(), yaw);
            if (!canOccupy(structure, before, next, -1, assembly, ignored)) return accepted;
            accepted = yaw;
        }
        return wantedYaw;
    }

    private static <T extends Entity & CollidableStructure> Vec3 clampMotion(
            T structure,
            Vec3 wanted,
            List<CollisionGroup> groups,
            double stepHeight,
            Predicate<CollidableStructure> ignored) {
        if (groups.isEmpty() || wanted.lengthSqr() <= MOVEMENT_EPSILON * MOVEMENT_EPSILON)
            return wanted;
        return clampMotion(structure, wanted, SupportedStructures.of(structure, groups, ignored), stepHeight, ignored);
    }

    /** Moves the structure with its loads, which are already gathered. */
    static <T extends Entity & CollidableStructure> Vec3 clampMotion(
            T structure,
            Vec3 wanted,
            SupportedStructures assembly,
            double stepHeight,
            Predicate<CollidableStructure> ignored) {
        if (wanted.lengthSqr() <= MOVEMENT_EPSILON * MOVEMENT_EPSILON) return wanted;
        return resolveMotion(
                structure,
                structure.collisionTransform(),
                assembly.groups(),
                wanted,
                stepHeight,
                other -> ignored.test(other) || assembly.contains(other));
    }

    private static <T extends Entity & CollidableStructure> Vec3 resolveMotion(
            T structure,
            StructureTransform transform,
            List<CollisionGroup> groups,
            Vec3 wanted,
            double stepHeight,
            Predicate<CollidableStructure> ignored) {
        AABB bounds = StructureCollisionResolver.worldBounds(transform, groups);
        if (bounds == null) return wanted;
        AABB path = bounds.expandTowards(wanted).inflate(0.01D);
        List<AABB> blocks =
                collectObstacles(
                        structure, path, stepHeight, groups, transform, offset(transform, wanted));
        List<CollidableStructure> nearby =
                StructureCollisionRegistry.structuresNear(structure, path, 0.01D).stream()
                        .filter(other -> !ignored.test(other))
                        .toList();
        Vec3 allowed = wanted;
        // Recheck earlier constraints after sliding against another body.
        for (int pass = 0; pass < 4; pass++) {
            Vec3 before = allowed;
            allowed =
                    StructureCollisionResolver.resolveStructureMovement(
                                    transform, groups, blocks, allowed)
                            .allowed();
            for (CollidableStructure other : nearby) {
                allowed =
                        StructureCollisionResolver.resolveStructureMovement(
                                        transform,
                                        groups,
                                        other.collisionTransform(),
                                        StructureGeometry.solid(other),
                                        allowed)
                                .allowed();
            }
            if (before.distanceToSqr(allowed) <= MOVEMENT_EPSILON * MOVEMENT_EPSILON) break;
        }
        return allowed;
    }

    static <T extends Entity & CollidableStructure> boolean isSupportedByWorld(
            T structure, double reach) {
        List<CollisionGroup> groups = StructureGeometry.supportContact(structure);
        AABB bounds = StructureCollisionResolver.worldBounds(structure, groups);
        if (bounds == null) return false;
        Vec3 down = new Vec3(0, -reach, 0);
        return StructureCollisionResolver.resolveStructureMovement(
                                structure.collisionTransform(),
                                groups,
                                collectObstacles(
                                        structure,
                                        bounds.expandTowards(down),
                                        -1,
                                        groups,
                                        structure.collisionTransform(),
                                        offset(structure.collisionTransform(), down)),
                                down)
                        .allowed()
                        .y
                > down.y + MOVEMENT_EPSILON;
    }

    public static <T extends Entity & CollidableStructure> Vec3 clampMovement(
            T structure, Vec3 wanted, double stepHeight, Predicate<CollisionGroup> excluded) {
        Vec3 horizontal = new Vec3(wanted.x, 0.0D, wanted.z);
        if (horizontal.lengthSqr() <= MOVEMENT_EPSILON) {
            return wanted;
        }
        List<CollisionGroup> groups = terrainGroups(structure, excluded);
        if (groups.isEmpty()) {
            return wanted;
        }
        Vec3 allowed = clampMotion(structure, horizontal, groups, stepHeight, other -> false);
        return new Vec3(allowed.x, wanted.y, allowed.z);
    }

    public static <T extends Entity & CollidableStructure> boolean canOccupy(
            T structure,
            StructureTransform current,
            StructureTransform wanted,
            double stepHeight,
            Predicate<CollisionGroup> excluded) {
        return canOccupy(structure, current, wanted, stepHeight, excluded, other -> false);
    }

    public static <T extends Entity & CollidableStructure> boolean canOccupy(
            T structure,
            StructureTransform current,
            StructureTransform wanted,
            double stepHeight,
            Predicate<CollisionGroup> excluded,
            Predicate<CollidableStructure> ignored) {
        List<CollisionGroup> groups =
                stepHeight < 0
                        ? StructureGeometry.solid(structure).stream()
                                .filter(group -> !excluded.test(group))
                                .toList()
                        : terrainGroups(structure, excluded);
        if (groups.isEmpty()) {
            return true;
        }
        return canOccupy(structure, current, wanted, stepHeight,
                SupportedStructures.of(structure, groups, ignored), ignored);
    }

    private static <T extends Entity & CollidableStructure> boolean canOccupy(
            T structure,
            StructureTransform current,
            StructureTransform wanted,
            double stepHeight,
            SupportedStructures assembly,
            Predicate<CollidableStructure> ignored) {
        List<CollisionGroup> groups = assembly.groups();
        AABB wantedBounds = StructureCollisionResolver.worldBounds(wanted, groups);
        AABB currentBounds = StructureCollisionResolver.worldBounds(current, groups);
        if (wantedBounds == null || currentBounds == null) {
            return true;
        }
        AABB search =
                new AABB(
                        Math.min(currentBounds.minX, wantedBounds.minX),
                        Math.min(currentBounds.minY, wantedBounds.minY),
                        Math.min(currentBounds.minZ, wantedBounds.minZ),
                        Math.max(currentBounds.maxX, wantedBounds.maxX),
                        Math.max(currentBounds.maxY, wantedBounds.maxY),
                        Math.max(currentBounds.maxZ, wantedBounds.maxZ));
        List<AABB> obstacles =
                collectObstacles(
                        structure, search.inflate(0.5D), stepHeight, groups, current, wanted);
        double wantedPenetration = penetration(wanted, groups, obstacles);
        double currentPenetration = penetration(current, groups, obstacles);
        if (wantedPenetration > PENETRATION_EPSILON
                && wantedPenetration + PENETRATION_EPSILON >= currentPenetration) return false;
        for (CollidableStructure other :
                StructureCollisionRegistry.structuresNear(structure, search, 0.01D)) {
            if (ignored.test(other) || assembly.contains(other)) continue;
            double after =
                    StructureCollisionResolver.resolveStructures(
                                    other.collisionTransform(),
                                    StructureGeometry.solid(other),
                                    wanted,
                                    groups,
                                    Vec3.ZERO)
                            .pushOut()
                            .lengthSqr();
            if (after <= PENETRATION_EPSILON) continue;
            double before =
                    StructureCollisionResolver.resolveStructures(
                                    other.collisionTransform(),
                                    StructureGeometry.solid(other),
                                    current,
                                    groups,
                                    Vec3.ZERO)
                            .pushOut()
                            .lengthSqr();
            if (after + PENETRATION_EPSILON >= before) return false;
        }
        return true;
    }

    public static <T extends Entity & CollidableStructure> void unstick(
            T structure, double stepHeight, Predicate<CollisionGroup> excluded) {
        List<CollisionGroup> groups = terrainGroups(structure, excluded);
        if (groups.isEmpty()) {
            return;
        }
        StructureTransform transform = structure.collisionTransform();
        AABB bounds = StructureCollisionResolver.worldBounds(transform, groups);
        if (bounds == null) {
            return;
        }
        List<AABB> obstacles = collectObstacles(structure, bounds.inflate(0.5D), stepHeight);
        if (obstacles.isEmpty()) {
            return;
        }

        Vec3 deepest = Vec3.ZERO;
        double deepestLength = 0.0D;
        for (AABB obstacle : obstacles) {
            Vec3 push =
                    StructureCollisionResolver.resolve(transform, groups, obstacle, Vec3.ZERO, 0.0D)
                            .pushOut();
            double length = push.lengthSqr();
            if (length > deepestLength) {
                deepestLength = length;
                deepest = push;
            }
        }
        if (deepestLength <= MINIMUM_ESCAPE * MINIMUM_ESCAPE) {
            return;
        }

        Vec3 escape = deepest.reverse();
        double length = Math.sqrt(deepestLength);
        if (length > MAX_UNSTICK_STEP) {
            escape = escape.scale(MAX_UNSTICK_STEP / length);
        }
        structure.setPos(
                structure.getX() + escape.x,
                structure.getY() + escape.y,
                structure.getZ() + escape.z);
    }

    private static double penetration(
            StructureTransform transform, List<CollisionGroup> groups, List<AABB> obstacles) {
        double total = 0.0D;
        for (AABB obstacle : obstacles) {
            Vec3 push =
                    StructureCollisionResolver.resolve(transform, groups, obstacle, Vec3.ZERO, 0.0D)
                            .pushOut();
            total += push.lengthSqr();
        }
        return total;
    }

    private static List<CollisionGroup> terrainGroups(
            CollidableStructure structure, Predicate<CollisionGroup> excluded) {
        List<CollisionGroup> current = StructureGeometry.solid(structure);
        if (current.isEmpty()) {
            return List.of();
        }
        List<CollisionGroup> previous = StructureGeometry.previousSolid(structure);
        List<CollisionGroup> still = new ArrayList<>(current.size());
        for (CollisionGroup group : current) {
            if (excluded.test(group)) {
                continue;
            }
            CollisionPose before = poseOf(previous, group.name());
            if (before == null || before.equals(group.pose())) {
                still.add(group);
            }
        }
        return still;
    }

    private static CollisionPose poseOf(List<CollisionGroup> groups, String name) {
        for (CollisionGroup group : groups) {
            if (group.name().equals(name)) {
                return group.pose();
            }
        }
        return null;
    }

    private static List<AABB> collectObstacles(Entity structure, AABB search, double stepHeight) {
        double steppableTop =
                stepHeight < 0.0D
                        ? Double.NEGATIVE_INFINITY
                        : structure.getY() + stepHeight + STEP_MARGIN;
        List<AABB> obstacles = new ArrayList<>();
        for (VoxelShape shape : structure.level().getBlockCollisions(structure, search)) {
            for (AABB box : shape.toAabbs()) {
                if (box.maxY <= steppableTop) {
                    continue;
                }
                obstacles.add(box);
            }
        }
        var border = structure.level().getWorldBorder();
        if (border != null) {
            if (search.minX < border.getMinX())
                obstacles.add(
                        new AABB(
                                search.minX - 1,
                                search.minY - 1,
                                search.minZ - 1,
                                border.getMinX(),
                                search.maxY + 1,
                                search.maxZ + 1));
            if (search.maxX > border.getMaxX())
                obstacles.add(
                        new AABB(
                                border.getMaxX(),
                                search.minY - 1,
                                search.minZ - 1,
                                search.maxX + 1,
                                search.maxY + 1,
                                search.maxZ + 1));
            if (search.minZ < border.getMinZ())
                obstacles.add(
                        new AABB(
                                search.minX - 1,
                                search.minY - 1,
                                search.minZ - 1,
                                search.maxX + 1,
                                search.maxY + 1,
                                border.getMinZ()));
            if (search.maxZ > border.getMaxZ())
                obstacles.add(
                        new AABB(
                                search.minX - 1,
                                search.minY - 1,
                                border.getMaxZ(),
                                search.maxX + 1,
                                search.maxY + 1,
                                search.maxZ + 1));
        }
        return obstacles;
    }

    /**
     * Broad phase only: retain every authored OBB, but do not scan the empty volume between them.
     */
    private static List<AABB> collectObstacles(
            Entity structure,
            AABB search,
            double stepHeight,
            List<CollisionGroup> groups,
            StructureTransform from,
            StructureTransform to) {
        if (search.getXsize() * search.getYsize() * search.getZsize() < 64)
            return collectObstacles(structure, search, stepHeight);
        var cells = new java.util.TreeSet<Long>();
        for (CollisionGroup group : groups)
            for (var part : group.parts()) {
                AABB a = StructureCollisionResolver.worldPartBounds(from, group, part);
                AABB b = StructureCollisionResolver.worldPartBounds(to, group, part);
                AABB swept = a.minmax(b).inflate(.5);
                for (int z = net.minecraft.util.Mth.floor(swept.minZ / 4);
                        z <= net.minecraft.util.Mth.floor(swept.maxZ / 4);
                        z++)
                    for (int y = net.minecraft.util.Mth.floor(swept.minY / 4);
                            y <= net.minecraft.util.Mth.floor(swept.maxY / 4);
                            y++)
                        for (int x = net.minecraft.util.Mth.floor(swept.minX / 4);
                                x <= net.minecraft.util.Mth.floor(swept.maxX / 4);
                                x++) cells.add(net.minecraft.core.BlockPos.asLong(x, y, z));
            }
        var obstacles = new java.util.LinkedHashSet<AABB>();
        for (AABB tile : TerrainQueryRegions.merge(cells)) {
            if (tile.intersects(search) && !emptyTerrainSections(structure, tile))
                obstacles.addAll(collectObstacles(structure, tile.intersect(search), stepHeight));
        }
        // World-border collision is independent of block-section contents.
        var border = structure.level().getWorldBorder();
        if (border != null
                && (search.minX < border.getMinX()
                        || search.maxX > border.getMaxX()
                        || search.minZ < border.getMinZ()
                        || search.maxZ > border.getMaxZ()))
            obstacles.addAll(collectObstacles(structure, search, stepHeight));
        return new ArrayList<>(obstacles);
    }

    private static boolean emptyTerrainSections(Entity entity, AABB tile) {
        var level = entity.level();
        var context = net.minecraft.world.phys.shapes.CollisionContext.of(entity);
        boolean solidFluids =
                context.canStandOnFluid(
                                net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState(),
                                net.minecraft.world.level.material.Fluids.WATER.defaultFluidState())
                        || context.canStandOnFluid(
                                net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState(),
                                net.minecraft.world.level.material.Fluids.LAVA.defaultFluidState());
        // Include neighbouring cells: fences, pistons and context-sensitive shapes may protrude.
        for (int z = net.minecraft.util.Mth.floor(tile.minZ - 1) >> 4;
                z <= net.minecraft.util.Mth.floor(tile.maxZ + 1) >> 4;
                z++)
            for (int x = net.minecraft.util.Mth.floor(tile.minX - 1) >> 4;
                    x <= net.minecraft.util.Mth.floor(tile.maxX + 1) >> 4;
                    x++) {
                var chunk = level.getChunkForCollisions(x, z);
                if (!(chunk instanceof net.minecraft.world.level.chunk.ChunkAccess)) return false;
                var sections = ((net.minecraft.world.level.chunk.ChunkAccess) chunk).getSections();
                for (int y = level.getSectionIndex(net.minecraft.util.Mth.floor(tile.minY - 1));
                        y <= level.getSectionIndex(net.minecraft.util.Mth.floor(tile.maxY + 1));
                        y++) {
                    if (y < 0 || y >= sections.length) continue;
                    var section = sections[y];
                    if (!section.hasOnlyAir()
                            && section.maybeHas(
                                    state ->
                                            !state.isAir()
                                                    && (solidFluids
                                                            || !(state.getBlock()
                                                                    instanceof
                                                                    net.minecraft.world.level.block
                                                                            .LiquidBlock))))
                        return false;
                }
            }
        return true;
    }
}
