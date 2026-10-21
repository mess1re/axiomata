package me.mss1r.axiomata.collision;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Authored cubes stay axis-aligned in their own local frames. Entity boxes are transformed into
// those frames for SAT tests, then normals and displacement are converted back to world space.
public final class StructureCollisionResolver {
    private static final double IMPACT_TIME_EPSILON = 1.0E-7D;
    private static final double MOVEMENT_EPSILON = 1.0E-12D;
    private static final double CONTACT_CLEARANCE = 1.0E-4D;
    private static final double WALKABLE_NORMAL_Y = 0.55D;
    private static final int MAX_MOVEMENT_PASSES = 8;
    private static final int GEOMETRY_CACHE_ENTRIES = 64, GEOMETRY_CACHE_PARTS = 16384;
    private static final ThreadLocal<java.util.ArrayDeque<PreparedGeometry>> GEOMETRY_CACHE =
            ThreadLocal.withInitial(java.util.ArrayDeque::new);

    private StructureCollisionResolver() {}

    public record Response(
            Vec3 pushOut,
            double allowedTime,
            List<Vec3> impactNormals,
            CollisionGroup groundGroup) {
        public static final Response FREE = new Response(Vec3.ZERO, 1.0D, List.of(), null);

        public boolean isBlocked() {
            return allowedTime < 1.0D || !pushOut.equals(Vec3.ZERO);
        }

        public boolean isSupported() {
            return groundGroup != null;
        }
    }

    public record ResolvedMovement(Vec3 allowed, CollisionGroup groundGroup) {
        public boolean isSupported() {
            return groundGroup != null;
        }
    }

    private record MovementPass(
            double allowedTime,
            List<Vec3> contactNormals,
            List<Vec3> impactNormals,
            CollisionGroup groundGroup) {
        private boolean isBlocked() {
            return allowedTime < 1.0D;
        }
    }

    public static ResolvedMovement resolveMovement(
            CollidableStructure structure, AABB worldBox, Vec3 worldMovement, double stepHeight) {
        return resolveMovement(
                structure.collisionTransform(),
                StructureGeometry.solid(structure),
                worldBox,
                worldMovement,
                stepHeight);
    }

    public static ResolvedMovement resolveStructureMovement(
            StructureTransform transform,
            List<CollisionGroup> groups,
            List<AABB> obstacles,
            Vec3 worldMovement) {
        return resolveMovement(transform, groups, obstacles, worldMovement, true);
    }

    /** Sweeps the moving geometry against the fixed geometry, preserving both sets of rotations. */
    public static ResolvedMovement resolveStructureMovement(
            StructureTransform movingTransform,
            List<CollisionGroup> movingGroups,
            StructureTransform fixedTransform,
            List<CollisionGroup> fixedGroups,
            Vec3 worldMovement) {
        // Two resting bodies touch along a seam: only parts whose sweep reaches the other body's
        // bounds can matter, on both sides. Without this a stack compared every part of every
        // neighbour on every pass.
        AABB fixedBounds = worldBounds(fixedTransform, fixedGroups);
        if (fixedBounds == null) return new ResolvedMovement(worldMovement, null);
        AABB fixedReach = fixedBounds.inflate(CONTACT_CLEARANCE);
        List<OrientedBox> near = new ArrayList<>();
        AABB nearBounds = null;
        for (OrientedBox box : worldBoxes(movingTransform, movingGroups)) {
            AABB swept = box.enclosingBounds().expandTowards(worldMovement);
            if (!swept.intersects(fixedReach)) continue;
            near.add(box);
            nearBounds = nearBounds == null ? swept : nearBounds.minmax(swept);
        }
        if (near.isEmpty()) return new ResolvedMovement(worldMovement, null);
        AABB reach = nearBounds.inflate(CONTACT_CLEARANCE);
        List<CollisionGroup> touched = new ArrayList<>();
        for (CollisionGroup group : fixedGroups) {
            List<CollisionPart> parts = new ArrayList<>();
            for (CollisionPart part : group.parts())
                if (worldPartBounds(fixedTransform, group, part).intersects(reach)) parts.add(part);
            if (parts.size() == group.parts().size()) touched.add(group);
            else if (!parts.isEmpty())
                touched.add(new CollisionGroup(group.name(), new CollisionShape(group.shape().pivot(), parts), group.pose()));
        }
        if (touched.isEmpty()) return new ResolvedMovement(worldMovement, null);
        return resolveOrientedMovement(fixedTransform, touched, near, worldMovement, false);
    }

    static ResolvedMovement resolveMovement(
            StructureTransform transform,
            List<CollisionGroup> groups,
            AABB worldBox,
            Vec3 worldMovement,
            double stepHeight) {
        return resolveMovement(transform, groups, List.of(worldBox), worldMovement, false);
    }

    private static ResolvedMovement resolveMovement(
            StructureTransform transform,
            List<CollisionGroup> groups,
            List<AABB> worldBoxes,
            Vec3 worldMovement,
            boolean structureMoves) {
        return resolveOrientedMovement(
                transform,
                groups,
                worldBoxes.stream().map(box -> OrientedBox.of(box, Rotation3.IDENTITY)).toList(),
                worldMovement,
                structureMoves);
    }

    private static ResolvedMovement resolveOrientedMovement(
            StructureTransform transform,
            List<CollisionGroup> groups,
            List<OrientedBox> worldBoxes,
            Vec3 worldMovement,
            boolean structureMoves) {
        if (groups.isEmpty() || worldBoxes.isEmpty())
            return new ResolvedMovement(worldMovement, null);
        Vec3 allowed = Vec3.ZERO;
        Vec3 remaining = worldMovement;
        CollisionGroup groundGroup = null;

        // Repeat after each hit so contact with the floor does not hide a wall reached later in
        // the same movement.
        for (int pass = 0; pass < MAX_MOVEMENT_PASSES; pass++) {
            Vec3 boxOffset = structureMoves ? allowed.reverse() : allowed;
            MovementPass response =
                    resolveMovementPass(
                            transform,
                            groups,
                            moveBoxes(worldBoxes, boxOffset),
                            remaining,
                            structureMoves);
            if (response.groundGroup() != null) {
                groundGroup = response.groundGroup();
            }

            Vec3 contactSlide = slideAlongSurfaces(remaining, response.contactNormals());
            if (contactSlide.distanceToSqr(remaining) > MOVEMENT_EPSILON) {
                remaining = contactSlide;
                if (remaining.lengthSqr() <= MOVEMENT_EPSILON) {
                    break;
                }
                continue;
            }
            if (!response.isBlocked()) {
                allowed = allowed.add(remaining);
                remaining = Vec3.ZERO;
                break;
            }

            double travelTime = contactSafeTravelTime(remaining, response);
            Vec3 travelled = remaining.scale(travelTime);
            allowed = allowed.add(travelled);
            Vec3 sliding =
                    slideAlongSurfaces(
                            remaining.scale(1.0D - travelTime), response.impactNormals());
            if (sliding.lengthSqr() <= MOVEMENT_EPSILON) {
                remaining = Vec3.ZERO;
                break;
            }
            if (travelled.lengthSqr() <= MOVEMENT_EPSILON
                    && sliding.distanceToSqr(remaining) <= MOVEMENT_EPSILON) {
                break;
            }
            remaining = sliding;
        }

        return new ResolvedMovement(allowed, groundGroup);
    }

    private static double contactSafeTravelTime(Vec3 movement, MovementPass response) {
        // Stop just short of the separating plane. Landing exactly on it can become a tiny overlap
        // next tick and produce a normal facing the wrong way.
        double safeTime = response.allowedTime();
        for (Vec3 normal : response.impactNormals()) {
            double inwardSpeed = -movement.dot(normal);
            if (inwardSpeed > MOVEMENT_EPSILON) {
                safeTime =
                        Math.min(
                                safeTime,
                                Math.max(
                                        0.0D,
                                        response.allowedTime() - CONTACT_CLEARANCE / inwardSpeed));
            }
        }
        return safeTime;
    }

    private static MovementPass resolveMovementPass(
            StructureTransform transform,
            List<CollisionGroup> groups,
            List<OrientedBox> worldBoxes,
            Vec3 worldMovement,
            boolean structureMoves) {
        // Moving the structure through fixed boxes is the same sweep with reversed relative motion.
        Vec3 relativeMovement = structureMoves ? worldMovement.reverse() : worldMovement;
        double allowedTime = 1.0D;
        List<Vec3> contactNormals = new java.util.ArrayList<>();
        List<Vec3> impactNormals = new java.util.ArrayList<>();
        CollisionGroup groundGroup = null;

        CollisionBoxIndex<OrientedBox> index =
                worldBoxes.size() > 8 ? indexForBoxes(worldBoxes) : null;
        for (CollisionGroup group : groups) {
            for (CollisionPart part : group.parts()) {
                Rotation3 worldToPart = worldToPart(transform, group, part);
                Vec3 movement = worldToPart.transform(relativeMovement);
                AABB box = part.box();
                AABB partPath =
                        worldBox(transform, group, part)
                                .enclosingBounds()
                                .expandTowards(
                                        structureMoves ? worldMovement : worldMovement.reverse())
                                .inflate(CONTACT_CLEARANCE);
                for (OrientedBox worldBox : index == null ? worldBoxes : index.query(partPath)) {
                    if (index == null && !partPath.intersects(worldBox.enclosingBounds())) continue;
                    OrientedBox moving =
                            new OrientedBox(
                                    toPartSpace(transform, group, part, worldBox.center()),
                                    worldBox.halfExtent(),
                                    worldToPart.multiply(worldBox.rotation()));
                    BoxSeparation separation = OrientedBoxCollider.separate(box, moving, movement);
                    if (separation == null) {
                        continue;
                    }

                    Vec3 localNormal = separation.contactNormal();
                    Vec3 worldNormal =
                            localNormal == null
                                    ? null
                                    : worldToPart.transformInverse(localNormal).normalize();
                    if (worldNormal != null && structureMoves) {
                        worldNormal = worldNormal.reverse();
                    }
                    boolean supporting = isUpward(worldNormal);
                    if (separation.isOverlapping()) {
                        addNormal(contactNormals, worldNormal);
                        if (supporting) {
                            groundGroup = group;
                        }
                        continue;
                    }

                    double timeOfImpact = separation.timeOfImpact();
                    if (timeOfImpact == BoxSeparation.NO_IMPACT) {
                        continue;
                    }
                    if (timeOfImpact < allowedTime - IMPACT_TIME_EPSILON) {
                        allowedTime = timeOfImpact;
                        impactNormals.clear();
                        addNormal(impactNormals, worldNormal);
                        groundGroup = supporting ? group : null;
                    } else if (Math.abs(timeOfImpact - allowedTime) <= IMPACT_TIME_EPSILON) {
                        addNormal(impactNormals, worldNormal);
                        if (supporting) {
                            groundGroup = group;
                        }
                    }
                }
            }
        }

        return new MovementPass(
                allowedTime, List.copyOf(contactNormals), List.copyOf(impactNormals), groundGroup);
    }

    private static boolean isUpward(Vec3 worldNormal) {
        return worldNormal != null && worldNormal.y > WALKABLE_NORMAL_Y;
    }

    private static List<OrientedBox> moveBoxes(List<OrientedBox> boxes, Vec3 offset) {
        if (offset.lengthSqr() <= MOVEMENT_EPSILON) {
            return boxes;
        }
        List<OrientedBox> moved = new java.util.ArrayList<>(boxes.size());
        for (OrientedBox box : boxes) {
            moved.add(box.moved(offset));
        }
        return moved;
    }

    private static Vec3 slideAlongSurfaces(Vec3 movement, List<Vec3> normals) {
        Vec3 sliding = movement;
        for (Vec3 normal : normals) {
            sliding = slideAlongSurface(sliding, normal);
        }
        return sliding;
    }

    private static void addNormal(List<Vec3> normals, Vec3 normal) {
        if (normal == null) {
            return;
        }
        for (Vec3 existing : normals) {
            if (existing.dot(normal) > 1.0D - IMPACT_TIME_EPSILON) {
                return;
            }
        }
        normals.add(normal);
    }

    private static Vec3 slideAlongSurface(Vec3 movement, Vec3 normal) {
        double inward = movement.dot(normal);
        if (inward >= 0.0D) {
            return movement;
        }
        if (normal.y > WALKABLE_NORMAL_Y) {
            // Preserve requested horizontal speed on walkable slopes. Orthogonal projection makes
            // gravity accelerate entities downhill and slows them on the way up.
            double surfaceY = -(movement.x * normal.x + movement.z * normal.z) / normal.y;
            return new Vec3(movement.x, Math.max(movement.y, surfaceY), movement.z);
        }
        return movement.subtract(normal.scale(inward));
    }

    public static Response resolve(
            CollidableStructure structure, AABB worldBox, Vec3 worldMovement, double stepHeight) {
        return resolve(
                structure.collisionTransform(),
                StructureGeometry.solid(structure),
                worldBox,
                worldMovement,
                stepHeight);
    }

    public static Response resolve(
            StructureTransform transform,
            List<CollisionGroup> groups,
            AABB worldBox,
            Vec3 worldMovement,
            double stepHeight) {
        return resolve(
                transform,
                groups,
                OrientedBox.of(worldBox, Rotation3.IDENTITY),
                worldMovement,
                stepHeight);
    }

    private static Response resolve(
            StructureTransform transform,
            List<CollisionGroup> groups,
            OrientedBox worldBox,
            Vec3 worldMovement,
            double stepHeight) {
        PreparedGeometry prepared = preparedGeometry(transform, groups);
        return resolvePrepared(
                prepared.parts,
                prepared.parts.size() > 8 ? prepared.partIndex() : null,
                worldBox,
                worldMovement,
                stepHeight);
    }

    private record FixedPart(
            int order,
            CollisionGroup group,
            CollisionPart part,
            Rotation3 worldToPart,
            Vec3 worldCenter,
            AABB bounds) {}

    private record GroupStamp(CollisionGroup group, List<CollisionPart> parts) {
        boolean matches(CollisionGroup current) {
            return group.name().equals(current.name())
                    && group.pose().equals(current.pose())
                    && group.shape().pivot().equals(current.shape().pivot())
                    && parts.equals(current.parts());
        }
    }

    /**
     * Bounded, thread-local value snapshots: no entity references and no stale mutable part lists.
     */
    private static final class PreparedGeometry {
        final StructureTransform transform;
        final List<GroupStamp> source;
        final List<FixedPart> parts;
        final List<OrientedBox> boxes;
        final AABB bounds;
        CollisionBoxIndex<FixedPart> partIndex;
        CollisionBoxIndex<OrientedBox> boxIndex;

        PreparedGeometry(StructureTransform transform, List<CollisionGroup> groups) {
            this.transform = transform;
            source = groups.stream().map(g -> new GroupStamp(g, List.copyOf(g.parts()))).toList();
            parts = prepareParts(transform, groups);
            boxes =
                    parts.stream()
                            .map(
                                    p ->
                                            new OrientedBox(
                                                    p.worldCenter,
                                                    OrientedBox.halfExtentOf(p.part.box()),
                                                    p.worldToPart.transpose()))
                            .toList();
            AABB combined = null;
            for (FixedPart part : parts)
                combined = combined == null ? part.bounds : combined.minmax(part.bounds);
            bounds = combined;
        }

        boolean matches(StructureTransform pose, List<CollisionGroup> groups) {
            if (!transform.equals(pose) || source.size() != groups.size()) return false;
            for (int i = 0; i < groups.size(); i++)
                if (!source.get(i).matches(groups.get(i))) return false;
            return true;
        }

        CollisionBoxIndex<FixedPart> partIndex() {
            if (partIndex == null) partIndex = new CollisionBoxIndex<>(parts, FixedPart::bounds);
            return partIndex;
        }

        CollisionBoxIndex<OrientedBox> boxIndex() {
            if (boxIndex == null)
                boxIndex = new CollisionBoxIndex<>(boxes, OrientedBox::enclosingBounds);
            return boxIndex;
        }
    }

    private static PreparedGeometry preparedGeometry(
            StructureTransform transform, List<CollisionGroup> groups) {
        var cache = GEOMETRY_CACHE.get();
        for (var iterator = cache.iterator(); iterator.hasNext(); ) {
            var value = iterator.next();
            if (value.matches(transform, groups)) {
                iterator.remove();
                cache.addFirst(value);
                return value;
            }
        }
        var value = new PreparedGeometry(transform, groups);
        // Room for both geometry lists of a few dozen moving structures: once a crowd outgrows the
        // list, every lookup misses and each structure is prepared again several times a tick.
        if (value.parts.size() <= GEOMETRY_CACHE_PARTS) {
            cache.addFirst(value);
            int total = cache.stream().mapToInt(g -> g.parts.size()).sum();
            while (cache.size() > GEOMETRY_CACHE_ENTRIES || total > GEOMETRY_CACHE_PARTS)
                total -= cache.removeLast().parts.size();
        }
        return value;
    }

    private static CollisionBoxIndex<OrientedBox> indexForBoxes(List<OrientedBox> boxes) {
        for (var value : GEOMETRY_CACHE.get()) if (value.boxes == boxes) return value.boxIndex();
        return new CollisionBoxIndex<>(boxes, OrientedBox::enclosingBounds);
    }

    private static List<FixedPart> prepareParts(
            StructureTransform transform, List<CollisionGroup> groups) {
        List<FixedPart> result = new java.util.ArrayList<>();
        for (CollisionGroup group : groups)
            for (CollisionPart part : group.parts()) {
                Rotation3 rotation = worldToPart(transform, group, part);
                Vec3 center = transform.toWorld(group.fromPart(part, part.box().getCenter()));
                AABB bounds =
                        new OrientedBox(
                                        center,
                                        OrientedBox.halfExtentOf(part.box()),
                                        rotation.transpose())
                                .enclosingBounds();
                result.add(new FixedPart(result.size(), group, part, rotation, center, bounds));
            }
        return result;
    }

    private static final class PartCursor {
        private final CollisionBoxIndex<FixedPart> index;
        private final OrientedBox box;
        private final Vec3 movement;
        private List<FixedPart> candidates;
        private Vec3 previousPush;
        private int cursor, lastOrder = -1;

        PartCursor(
                List<FixedPart> direct,
                CollisionBoxIndex<FixedPart> index,
                OrientedBox box,
                Vec3 movement) {
            this.index = index;
            this.box = box;
            this.movement = movement;
            candidates = index == null ? direct : null;
        }

        FixedPart next(Vec3 push) {
            if (index != null && !push.equals(previousPush)) {
                // Depenetration may expose a later part. Re-query, retaining original processing
                // order.
                candidates =
                        index.query(
                                box.moved(push)
                                        .enclosingBounds()
                                        .expandTowards(movement)
                                        .inflate(CONTACT_CLEARANCE));
                previousPush = push;
                cursor = 0;
            }
            while (cursor < candidates.size()) {
                var next = candidates.get(cursor++);
                if (next.order > lastOrder) {
                    lastOrder = next.order;
                    return next;
                }
            }
            return null;
        }
    }

    private static Response resolvePrepared(
            List<FixedPart> parts,
            CollisionBoxIndex<FixedPart> index,
            OrientedBox worldBox,
            Vec3 worldMovement,
            double stepHeight) {
        Vec3 accumulatedPush = Vec3.ZERO;
        double allowedTime = 1.0D;
        List<Vec3> impactNormals = new java.util.ArrayList<>();
        CollisionGroup groundGroup = null;

        PartCursor cursor = new PartCursor(parts, index, worldBox, worldMovement);
        FixedPart fixed;
        while ((fixed = cursor.next(accumulatedPush)) != null) {
            CollisionGroup group = fixed.group;
            CollisionPart part = fixed.part;
            Rotation3 worldToPart = fixed.worldToPart;
            AABB partBounds = fixed.bounds;
            if (!partBounds
                    .inflate(CONTACT_CLEARANCE)
                    .intersects(
                            worldBox.moved(accumulatedPush)
                                    .enclosingBounds()
                                    .expandTowards(worldMovement))) {
                continue;
            }
            Vec3 center =
                    part.box()
                            .getCenter()
                            .add(
                                    worldToPart.transform(
                                            worldBox.center()
                                                    .subtract(fixed.worldCenter)
                                                    .add(accumulatedPush)));
            OrientedBox moving =
                    new OrientedBox(
                            center,
                            worldBox.halfExtent(),
                            worldToPart.multiply(worldBox.rotation()));
            Vec3 movement = worldToPart.transform(worldMovement);
            AABB box = part.box();
            BoxSeparation separation = OrientedBoxCollider.separate(box, moving, movement);
            if (separation == null) {
                continue;
            }

            if (separation.isOverlapping()) {
                Vec3 push = separation.pushOut(stepHeight);
                if (push == null || push.equals(Vec3.ZERO)) {
                    continue;
                }
                moving = moving.moved(push);
                Vec3 worldPush = worldToPart.transformInverse(push);
                accumulatedPush = accumulatedPush.add(worldPush);
                if (isUpward(worldToPart, separation, worldPush)) {
                    groundGroup = group;
                }
                continue;
            }

            double timeOfImpact = separation.timeOfImpact();
            if (timeOfImpact == BoxSeparation.NO_IMPACT) {
                continue;
            }

            Vec3 localNormal = separation.contactNormal();
            Vec3 worldNormal =
                    localNormal == null
                            ? null
                            : worldToPart.transformInverse(localNormal).normalize();
            if (timeOfImpact < allowedTime - IMPACT_TIME_EPSILON) {
                allowedTime = timeOfImpact;
                impactNormals.clear();
                if (worldNormal != null) {
                    impactNormals.add(worldNormal);
                }
                groundGroup = isUpward(worldToPart, separation, Vec3.ZERO) ? group : null;
            } else if (Math.abs(timeOfImpact - allowedTime) <= IMPACT_TIME_EPSILON) {
                if (worldNormal != null) {
                    impactNormals.add(worldNormal);
                }
                if (isUpward(worldToPart, separation, Vec3.ZERO)) {
                    groundGroup = group;
                }
            }
        }

        if (accumulatedPush.equals(Vec3.ZERO) && allowedTime == 1.0D && groundGroup == null) {
            return Response.FREE;
        }
        return new Response(accumulatedPush, allowedTime, List.copyOf(impactNormals), groundGroup);
    }

    /**
     * Depenetration and contact query for two compound structures. Normals push the moving one out.
     */
    public static Response resolveStructures(
            StructureTransform fixedTransform,
            List<CollisionGroup> fixedGroups,
            StructureTransform movingTransform,
            List<CollisionGroup> movingGroups,
            Vec3 worldMovement) {
        Vec3 push = Vec3.ZERO;
        double time = 1.0D;
        List<Vec3> normals = new java.util.ArrayList<>();
        CollisionGroup support = null;
        PreparedGeometry prepared = preparedGeometry(fixedTransform, fixedGroups);
        List<FixedPart> parts = prepared.parts;
        CollisionBoxIndex<FixedPart> fixedIndex = prepared.partIndex();
        for (OrientedBox box : worldBoxes(movingTransform, movingGroups)) {
            if (!fixedIndex.any(
                    box.moved(push)
                            .enclosingBounds()
                            .expandTowards(worldMovement)
                            .inflate(CONTACT_CLEARANCE))) continue;
            Response response =
                    resolvePrepared(parts, fixedIndex, box.moved(push), worldMovement, 0.0D);
            push = push.add(response.pushOut());
            if (response.allowedTime() < time - IMPACT_TIME_EPSILON) {
                time = response.allowedTime();
                normals.clear();
                support = response.groundGroup();
            }
            if (Math.abs(response.allowedTime() - time) <= IMPACT_TIME_EPSILON) {
                for (Vec3 normal : response.impactNormals()) addNormal(normals, normal);
                if (response.isSupported()) support = response.groundGroup();
            }
        }
        return new Response(push, time, List.copyOf(normals), support);
    }

    public static CollisionGroup supportingGroup(
            StructureTransform fixedTransform,
            List<CollisionGroup> fixedGroups,
            StructureTransform movingTransform,
            List<CollisionGroup> movingGroups,
            double reach) {
        // Support only depends on the pose of one body relative to the other, so a stack carried
        // rigidly keeps its answers from tick to tick instead of sweeping every pair again.
        Vec3 offset = fixedTransform.toLocal(new Vec3(movingTransform.x(), movingTransform.y(), movingTransform.z()));
        float turn = net.minecraft.util.Mth.wrapDegrees(movingTransform.yawDegrees() - fixedTransform.yawDegrees());
        var cache = SUPPORT_CACHE.get();
        var key = new SupportPair(fixedGroups, movingGroups);
        SupportAnswer answer = cache.get(key);
        if (answer != null && answer.matches(fixedGroups, movingGroups, offset, turn, reach)) {
            return answer.group() < 0 ? null : fixedGroups.get(answer.group());
        }
        // Use the movement solver: a side overlap is not evidence of support underneath.
        CollisionGroup support = resolveStructureMovement(
                        movingTransform,
                        movingGroups,
                        fixedTransform,
                        fixedGroups,
                        new Vec3(0.0D, -Math.max(0.0D, reach), 0.0D))
                .groundGroup();
        int index = -1;
        if (support != null) {
            for (int i = 0; i < fixedGroups.size() && index < 0; i++) {
                if (fixedGroups.get(i).name().equals(support.name())) index = i;
            }
        }
        cache.put(key, new SupportAnswer(stamps(fixedGroups), stamps(movingGroups), offset, turn, reach, index));
        return index < 0 ? support : fixedGroups.get(index);
    }

    private static final int SUPPORT_CACHE_SIZE = 256;
    private static final ThreadLocal<java.util.LinkedHashMap<SupportPair, SupportAnswer>> SUPPORT_CACHE =
            ThreadLocal.withInitial(() -> new java.util.LinkedHashMap<>(64, .75F, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<SupportPair, SupportAnswer> eldest) {
                    return size() > SUPPORT_CACHE_SIZE;
                }
            });

    /** Keyed by the identity of the two group lists; their contents are verified on every hit. */
    private record SupportPair(List<CollisionGroup> fixed, List<CollisionGroup> moving) {
        @Override
        public boolean equals(Object other) {
            return other instanceof SupportPair pair && pair.fixed == fixed && pair.moving == moving;
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(fixed) + System.identityHashCode(moving);
        }
    }

    private record SupportAnswer(List<GroupStamp> fixed, List<GroupStamp> moving, Vec3 offset, float turn,
                                 double reach, int group) {
        boolean matches(List<CollisionGroup> fixedGroups, List<CollisionGroup> movingGroups, Vec3 now,
                        float nowTurn, double nowReach) {
            return nowReach == reach && now.distanceToSqr(offset) < 1.0E-12
                    && Math.abs(net.minecraft.util.Mth.wrapDegrees(nowTurn - turn)) < 1.0E-4F
                    && stamped(fixed, fixedGroups) && stamped(moving, movingGroups);
        }
    }

    private static List<GroupStamp> stamps(List<CollisionGroup> groups) {
        return groups.stream().map(g -> new GroupStamp(g, List.copyOf(g.parts()))).toList();
    }

    private static boolean stamped(List<GroupStamp> stamps, List<CollisionGroup> groups) {
        if (stamps.size() != groups.size()) return false;
        for (int i = 0; i < groups.size(); i++) if (!stamps.get(i).matches(groups.get(i))) return false;
        return true;
    }

    private static List<OrientedBox> worldBoxes(
            StructureTransform transform, List<CollisionGroup> groups) {
        return preparedGeometry(transform, groups).boxes;
    }

    private static OrientedBox worldBox(
            StructureTransform transform, CollisionGroup group, CollisionPart part) {
        return new OrientedBox(
                transform.toWorld(group.fromPart(part, part.box().getCenter())),
                OrientedBox.halfExtentOf(part.box()),
                worldToPart(transform, group, part).transpose());
    }

    /** Conservative terrain broad-phase bounds of one fully transformed authored part. */
    public static AABB worldPartBounds(
            StructureTransform transform, CollisionGroup group, CollisionPart part) {
        return worldBox(transform, group, part).enclosingBounds();
    }

    public static CollisionGroup supportingGroup(
            CollidableStructure structure, AABB worldBox, double reach) {
        return supportingGroup(
                structure.collisionTransform(), StructureGeometry.solid(structure), worldBox, reach);
    }

    public static CollisionGroup supportingGroup(
            StructureTransform transform,
            List<CollisionGroup> groups,
            AABB worldBox,
            double reach) {
        Response response =
                resolve(transform, groups, worldBox, new Vec3(0.0D, -reach, 0.0D), 0.0D);
        return response.groundGroup();
    }

    public static AABB worldBounds(CollidableStructure structure) {
        return worldBounds(structure.collisionTransform(), structure.collisionGroups());
    }

    public static AABB worldBounds(CollidableStructure structure, List<CollisionGroup> groups) {
        return worldBounds(structure.collisionTransform(), groups);
    }

    public static AABB worldBounds(StructureTransform transform, List<CollisionGroup> groups) {
        return preparedGeometry(transform, groups).bounds;
    }

    public static boolean intersects(
            StructureTransform transform,
            List<CollisionGroup> groups,
            AABB worldBox,
            double reach) {
        for (CollisionGroup group : groups) {
            for (CollisionPart part : group.parts()) {
                Rotation3 worldToPart = worldToPart(transform, group, part);
                OrientedBox moving =
                        new OrientedBox(
                                toPartSpace(transform, group, part, worldBox.getCenter()),
                                OrientedBox.halfExtentOf(worldBox),
                                worldToPart);
                AABB box = part.box();
                BoxSeparation separation =
                        OrientedBoxCollider.separate(box.inflate(reach), moving, Vec3.ZERO);
                if (separation != null && separation.isOverlapping()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean intersects(
            CollidableStructure structure,
            List<CollisionGroup> groups,
            AABB worldBox,
            double reach) {
        return intersects(structure.collisionTransform(), groups, worldBox, reach);
    }

    public static boolean intersects(
            CollidableStructure first,
            List<CollisionGroup> firstGroups,
            CollidableStructure second,
            List<CollisionGroup> secondGroups,
            double reach) {
        StructureTransform firstTransform = first.collisionTransform();
        StructureTransform secondTransform = second.collisionTransform();
        Rotation3 firstLocalToWorld = firstTransform.worldToLocalRotation().transpose();

        for (CollisionGroup secondGroup : secondGroups) {
            for (CollisionPart secondPart : secondGroup.parts()) {
                Rotation3 worldToSecondPart = worldToPart(secondTransform, secondGroup, secondPart);
                AABB fixed = secondPart.box().inflate(Math.max(0.0D, reach));

                for (CollisionGroup firstGroup : firstGroups) {
                    for (CollisionPart firstPart : firstGroup.parts()) {
                        Vec3 firstCenterWorld =
                                firstTransform.toWorld(
                                        firstGroup.fromPart(
                                                firstPart, firstPart.box().getCenter()));
                        Vec3 firstCenterInSecondPart =
                                toPartSpace(
                                        secondTransform, secondGroup, secondPart, firstCenterWorld);
                        Rotation3 firstPartToSecondPart =
                                worldToSecondPart
                                        .multiply(firstLocalToWorld)
                                        .multiply(
                                                firstGroup
                                                        .structureToPartRotation(firstPart)
                                                        .transpose());
                        OrientedBox moving =
                                new OrientedBox(
                                        firstCenterInSecondPart,
                                        OrientedBox.halfExtentOf(firstPart.box()),
                                        firstPartToSecondPart);
                        BoxSeparation separation =
                                OrientedBoxCollider.separate(fixed, moving, Vec3.ZERO);
                        if (separation != null && separation.isOverlapping()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static Optional<Vec3> clipSegment(
            CollidableStructure structure, Vec3 worldStart, Vec3 worldEnd, double padding) {
        return clipSegment(structure.collisionTransform(), structure.collisionGroups(), worldStart, worldEnd, padding);
    }

    /** Clips only the supplied geometry, e.g. solid deck parts rather than decorative rigging. */
    public static Optional<Vec3> clipSegment(
            StructureTransform transform, List<CollisionGroup> groups,
            Vec3 worldStart, Vec3 worldEnd, double padding) {
        Vec3 structureStart = transform.toLocal(worldStart);
        Vec3 structureEnd = transform.toLocal(worldEnd);
        Vec3 closest = null;
        double closestDistanceSqr = Double.MAX_VALUE;
        for (CollisionGroup group : groups) {
            for (CollisionPart part : group.parts()) {
                Vec3 start = group.toPart(part, structureStart);
                Vec3 end = group.toPart(part, structureEnd);
                AABB box = part.box().inflate(Math.max(0.0D, padding));
                Optional<Vec3> localHit =
                        box.contains(start) ? Optional.of(start) : box.clip(start, end);
                if (localHit.isEmpty()) {
                    continue;
                }
                Vec3 worldHit = transform.toWorld(group.fromPart(part, localHit.get()));
                double distanceSqr = worldStart.distanceToSqr(worldHit);
                if (distanceSqr < closestDistanceSqr) {
                    closest = worldHit;
                    closestDistanceSqr = distanceSqr;
                }
            }
        }
        return Optional.ofNullable(closest);
    }

    public static Optional<Vec3> clipSegment(
            CollidableStructure structure, Vec3 worldStart, Vec3 worldEnd) {
        return clipSegment(structure, worldStart, worldEnd, 0.0D);
    }

    public static double distanceToSqr(CollidableStructure structure, Vec3 worldPoint) {
        StructureTransform transform = structure.collisionTransform();
        Vec3 structurePoint = transform.toLocal(worldPoint);
        double closestDistanceSqr = Double.MAX_VALUE;
        for (CollisionGroup group : structure.collisionGroups()) {
            for (CollisionPart part : group.parts()) {
                Vec3 point = group.toPart(part, structurePoint);
                closestDistanceSqr = Math.min(closestDistanceSqr, part.box().distanceToSqr(point));
            }
        }
        return closestDistanceSqr;
    }

    public static boolean intersectsSegment(
            CollidableStructure structure, Vec3 worldStart, Vec3 worldEnd) {
        return clipSegment(structure, worldStart, worldEnd).isPresent();
    }

    private static Rotation3 worldToPart(
            StructureTransform transform, CollisionGroup group, CollisionPart part) {
        Rotation3 yaw = transform.worldToLocalRotation();
        return group.structureToPartRotation(part).multiply(yaw);
    }

    private static Vec3 toPartSpace(
            StructureTransform transform, CollisionGroup group, CollisionPart part, Vec3 world) {
        return group.toPart(part, transform.toLocal(world));
    }

    private static boolean isUpward(
            Rotation3 worldToGroup, BoxSeparation separation, Vec3 worldPush) {
        Vec3 localNormal = separation.contactNormal();
        if (localNormal != null) {
            Vec3 worldNormal = worldToGroup.transformInverse(localNormal).normalize();
            return worldNormal.y > WALKABLE_NORMAL_Y;
        }
        return worldPush.y > 0.0D
                && Math.abs(worldPush.y) > Math.abs(worldPush.x) + Math.abs(worldPush.z);
    }
}
