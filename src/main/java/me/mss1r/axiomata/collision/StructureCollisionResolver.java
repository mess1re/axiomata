package me.mss1r.axiomata.collision;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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

    private StructureCollisionResolver() {
    }

    public record Response(Vec3 pushOut, double allowedTime, List<Vec3> impactNormals,
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

    private record MovementPass(double allowedTime, List<Vec3> contactNormals,
                                List<Vec3> impactNormals, CollisionGroup groundGroup) {
        private boolean isBlocked() {
            return allowedTime < 1.0D;
        }
    }

    public static ResolvedMovement resolveMovement(CollidableStructure structure, AABB worldBox,
                                                   Vec3 worldMovement, double stepHeight) {
        return resolveMovement(structure.collisionTransform(), structure.solidCollisionGroups(),
                worldBox, worldMovement, stepHeight);
    }

    public static ResolvedMovement resolveStructureMovement(StructureTransform transform,
                                                            List<CollisionGroup> groups,
                                                            List<AABB> obstacles, Vec3 worldMovement) {
        return resolveMovement(transform, groups, obstacles, worldMovement, true);
    }

    static ResolvedMovement resolveMovement(StructureTransform transform, List<CollisionGroup> groups,
                                             AABB worldBox, Vec3 worldMovement, double stepHeight) {
        return resolveMovement(transform, groups, List.of(worldBox), worldMovement, false);
    }

    private static ResolvedMovement resolveMovement(StructureTransform transform,
                                                    List<CollisionGroup> groups, List<AABB> worldBoxes,
                                                    Vec3 worldMovement, boolean structureMoves) {
        Vec3 allowed = Vec3.ZERO;
        Vec3 remaining = worldMovement;
        CollisionGroup groundGroup = null;

        // Repeat after each hit so contact with the floor does not hide a wall reached later in
        // the same movement.
        for (int pass = 0; pass < MAX_MOVEMENT_PASSES; pass++) {
            Vec3 boxOffset = structureMoves ? allowed.reverse() : allowed;
            MovementPass response = resolveMovementPass(
                    transform, groups, moveBoxes(worldBoxes, boxOffset), remaining, structureMoves);
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
            Vec3 sliding = slideAlongSurfaces(
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
                safeTime = Math.min(safeTime,
                        Math.max(0.0D, response.allowedTime() - CONTACT_CLEARANCE / inwardSpeed));
            }
        }
        return safeTime;
    }

    private static MovementPass resolveMovementPass(StructureTransform transform,
                                                    List<CollisionGroup> groups,
                                                    List<AABB> worldBoxes, Vec3 worldMovement,
                                                    boolean structureMoves) {
        // Moving the structure through fixed boxes is the same sweep with reversed relative motion.
        Vec3 relativeMovement = structureMoves ? worldMovement.reverse() : worldMovement;
        double allowedTime = 1.0D;
        List<Vec3> contactNormals = new java.util.ArrayList<>();
        List<Vec3> impactNormals = new java.util.ArrayList<>();
        CollisionGroup groundGroup = null;

        for (CollisionGroup group : groups) {
            for (CollisionPart part : group.parts()) {
                Rotation3 worldToPart = worldToPart(transform, group, part);
                Vec3 movement = worldToPart.transform(relativeMovement);
                AABB box = part.box();
                for (AABB worldBox : worldBoxes) {
                    OrientedBox moving = new OrientedBox(
                            toPartSpace(transform, group, part, worldBox.getCenter()),
                            OrientedBox.halfExtentOf(worldBox), worldToPart);
                    BoxSeparation separation = OrientedBoxCollider.separate(box, moving, movement);
                    if (separation == null) {
                        continue;
                    }

                    Vec3 localNormal = separation.contactNormal();
                    Vec3 worldNormal = localNormal == null
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

        return new MovementPass(allowedTime, List.copyOf(contactNormals),
                List.copyOf(impactNormals), groundGroup);
    }

    private static boolean isUpward(Vec3 worldNormal) {
        return worldNormal != null && worldNormal.y > WALKABLE_NORMAL_Y;
    }

    private static List<AABB> moveBoxes(List<AABB> boxes, Vec3 offset) {
        if (offset.lengthSqr() <= MOVEMENT_EPSILON) {
            return boxes;
        }
        List<AABB> moved = new java.util.ArrayList<>(boxes.size());
        for (AABB box : boxes) {
            moved.add(box.move(offset));
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

    public static Response resolve(CollidableStructure structure, AABB worldBox, Vec3 worldMovement,
                                   double stepHeight) {
        return resolve(structure.collisionTransform(), structure.solidCollisionGroups(),
                worldBox, worldMovement, stepHeight);
    }

    public static Response resolve(StructureTransform transform, List<CollisionGroup> groups,
                            AABB worldBox, Vec3 worldMovement, double stepHeight) {
        Vec3 accumulatedPush = Vec3.ZERO;
        double allowedTime = 1.0D;
        List<Vec3> impactNormals = new java.util.ArrayList<>();
        CollisionGroup groundGroup = null;

        for (CollisionGroup group : groups) {
            for (CollisionPart part : group.parts()) {
                Rotation3 worldToPart = worldToPart(transform, group, part);
                Vec3 center = toPartSpace(transform, group, part, worldBox.getCenter()).add(
                        worldToPart.transform(accumulatedPush));
                OrientedBox moving = new OrientedBox(center, OrientedBox.halfExtentOf(worldBox), worldToPart);
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
                Vec3 worldNormal = localNormal == null
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
        }

        if (accumulatedPush.equals(Vec3.ZERO) && allowedTime == 1.0D && groundGroup == null) {
            return Response.FREE;
        }
        return new Response(accumulatedPush, allowedTime, List.copyOf(impactNormals), groundGroup);
    }

    public static CollisionGroup supportingGroup(CollidableStructure structure, AABB worldBox,
                                                 double reach) {
        return supportingGroup(structure.collisionTransform(), structure.solidCollisionGroups(),
                worldBox, reach);
    }

    public static CollisionGroup supportingGroup(StructureTransform transform, List<CollisionGroup> groups,
                                          AABB worldBox, double reach) {
        Response response = resolve(transform, groups, worldBox, new Vec3(0.0D, -reach, 0.0D), 0.0D);
        return response.groundGroup();
    }

    public static AABB worldBounds(CollidableStructure structure) {
        return worldBounds(structure.collisionTransform(), structure.collisionGroups());
    }

    public static AABB worldBounds(CollidableStructure structure, List<CollisionGroup> groups) {
        return worldBounds(structure.collisionTransform(), groups);
    }

    public static AABB worldBounds(StructureTransform transform, List<CollisionGroup> groups) {
        AABB bounds = null;
        for (CollisionGroup group : groups) {
            for (CollisionPart part : group.parts()) {
                AABB box = part.box();
                for (double x : new double[]{box.minX, box.maxX}) {
                    for (double y : new double[]{box.minY, box.maxY}) {
                        for (double z : new double[]{box.minZ, box.maxZ}) {
                            Vec3 world = transform.toWorld(group.fromPart(part, new Vec3(x, y, z)));
                            AABB point = new AABB(world, world);
                            bounds = bounds == null ? point : bounds.minmax(point);
                        }
                    }
                }
            }
        }
        return bounds;
    }

    public static boolean intersects(StructureTransform transform, List<CollisionGroup> groups,
                              AABB worldBox, double reach) {
        for (CollisionGroup group : groups) {
            for (CollisionPart part : group.parts()) {
                Rotation3 worldToPart = worldToPart(transform, group, part);
                OrientedBox moving = new OrientedBox(
                        toPartSpace(transform, group, part, worldBox.getCenter()),
                        OrientedBox.halfExtentOf(worldBox), worldToPart);
                AABB box = part.box();
                BoxSeparation separation = OrientedBoxCollider.separate(box.inflate(reach), moving, Vec3.ZERO);
                if (separation != null && separation.isOverlapping()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean intersects(CollidableStructure structure, List<CollisionGroup> groups,
                                     AABB worldBox, double reach) {
        return intersects(structure.collisionTransform(), groups, worldBox, reach);
    }

    public static boolean intersects(CollidableStructure first, List<CollisionGroup> firstGroups,
                                     CollidableStructure second, List<CollisionGroup> secondGroups,
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
                        Vec3 firstCenterWorld = firstTransform.toWorld(
                                firstGroup.fromPart(firstPart, firstPart.box().getCenter()));
                        Vec3 firstCenterInSecondPart = toPartSpace(
                                secondTransform, secondGroup, secondPart, firstCenterWorld);
                        Rotation3 firstPartToSecondPart = worldToSecondPart
                                .multiply(firstLocalToWorld)
                                .multiply(firstGroup.structureToPartRotation(firstPart).transpose());
                        OrientedBox moving = new OrientedBox(
                                firstCenterInSecondPart,
                                OrientedBox.halfExtentOf(firstPart.box()),
                                firstPartToSecondPart);
                        BoxSeparation separation = OrientedBoxCollider.separate(
                                fixed, moving, Vec3.ZERO);
                        if (separation != null && separation.isOverlapping()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static Optional<Vec3> clipSegment(CollidableStructure structure, Vec3 worldStart, Vec3 worldEnd,
                                             double padding) {
        StructureTransform transform = structure.collisionTransform();
        Vec3 structureStart = transform.toLocal(worldStart);
        Vec3 structureEnd = transform.toLocal(worldEnd);
        Vec3 closest = null;
        double closestDistanceSqr = Double.MAX_VALUE;
        for (CollisionGroup group : structure.collisionGroups()) {
            for (CollisionPart part : group.parts()) {
                Vec3 start = group.toPart(part, structureStart);
                Vec3 end = group.toPart(part, structureEnd);
                AABB box = part.box().inflate(Math.max(0.0D, padding));
                Optional<Vec3> localHit = box.contains(start) ? Optional.of(start) : box.clip(start, end);
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

    public static Optional<Vec3> clipSegment(CollidableStructure structure, Vec3 worldStart, Vec3 worldEnd) {
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

    public static boolean intersectsSegment(CollidableStructure structure, Vec3 worldStart, Vec3 worldEnd) {
        return clipSegment(structure, worldStart, worldEnd).isPresent();
    }

    private static Rotation3 worldToPart(StructureTransform transform, CollisionGroup group,
                                         CollisionPart part) {
        Rotation3 yaw = transform.worldToLocalRotation();
        return group.structureToPartRotation(part).multiply(yaw);
    }

    private static Vec3 toPartSpace(StructureTransform transform, CollisionGroup group,
                                    CollisionPart part, Vec3 world) {
        return group.toPart(part, transform.toLocal(world));
    }

    private static boolean isUpward(Rotation3 worldToGroup, BoxSeparation separation, Vec3 worldPush) {
        Vec3 localNormal = separation.contactNormal();
        if (localNormal != null) {
            Vec3 worldNormal = worldToGroup.transformInverse(localNormal).normalize();
            return worldNormal.y > WALKABLE_NORMAL_Y;
        }
        return worldPush.y > 0.0D
                && Math.abs(worldPush.y) > Math.abs(worldPush.x) + Math.abs(worldPush.z);
    }
}
