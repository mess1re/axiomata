package me.mss1r.axiomata.collision.system;

import me.mss1r.axiomata.collision.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import com.google.common.collect.MapMaker;

/** Compound structures are not ordinary AABBs: both contact and carried motion use their solid parts. */
final class StructureCarrySystem {
    private static final double REACH = 0.03D;
    private static final double EPSILON = 1.0E-8D;
    private static final Map<CollidableStructure, WeakReference<CollidableStructure>> SUPPORTS = new MapMaker().weakKeys().makeMap();
    private static final Map<Entity, Long> CARRIED = new MapMaker().weakKeys().makeMap();

    private StructureCarrySystem() {
    }

    /** Whether a supporting structure has moved this one since the given game tick. */
    static boolean carriedSince(Entity structure, long tick) {
        Long carried = CARRIED.get(structure);
        return carried != null && carried >= tick;
    }

    static void tick(Entity parentEntity, CollidableStructure parent, AABB search,
                     StructureTransform previous, List<CollisionGroup> previousGroups,
                     StructureTransform current, List<CollisionGroup> currentGroups) {
        for (CollidableStructure other : StructureCollisionRegistry.structuresNear(parentEntity, search, REACH)) {
            if (other instanceof Entity entity && entity.isAlive() && !entity.noPhysics && !entity.isPassenger()
                    && !entity.level().isClientSide) {
                carry((Entity & CollidableStructure) entity, parent, previous, previousGroups, current, currentGroups);
            }
        }
    }

    private static <T extends Entity & CollidableStructure> void carry(
            T child, CollidableStructure parent, StructureTransform previous, List<CollisionGroup> previousGroups,
            StructureTransform current, List<CollisionGroup> currentGroups) {
        StructureTransform initial = child.collisionTransform();
        Predicate<CollidableStructure> notParent = other -> other == parent;
        SupportedStructures assembly = null;
        CollisionGroup support = child.getDeltaMovement().y <= 1.0E-4D
                ? StructureCollisionResolver.supportingGroup(previous, previousGroups,
                    child.collisionTransform(), StructureGeometry.solid(child), REACH) : null;
        boolean worldSupport = support != null && StructureTerrainCollision.isSupportedByWorld(child, REACH);
        if (support != null && !worldSupport && claimSupport(child, parent)) {
            CollisionGroup after = currentGroups.stream().filter(g -> g.name().equals(support.name())).findFirst().orElse(null);
            if (after != null) {
                StructureTransform wanted = StructureCarryTransform.carry(previous, support, current, after, child.collisionTransform());
                Vec3 movement = new Vec3(wanted.x() - initial.x(), wanted.y() - initial.y(), wanted.z() - initial.z());
                assembly = SupportedStructures.of(child, StructureGeometry.solid(child), notParent);
                Vec3 allowed = StructureTerrainCollision.clampMotion(child, movement, assembly, -1.0D, notParent);
                child.setPos(child.getX() + allowed.x, child.getY() + allowed.y, child.getZ() + allowed.z);
                float yaw = StructureTerrainCollision.clampYaw(child, wanted.yawDegrees(), assembly, notParent);
                child.rotateWithSupport(Mth.wrapDegrees(yaw - child.collisionTransform().yawDegrees()));
                if (allowed.distanceToSqr(movement) <= EPSILON) StructureCollisionSystem.markSupported(child);
            }
        }
        // A moving hull can also meet an unsupported structure from the side.
        Vec3 push = StructureCollisionResolver.resolveStructures(current, currentGroups,
                child.collisionTransform(), StructureGeometry.solid(child), Vec3.ZERO).pushOut();
        if (push.lengthSqr() > EPSILON) {
            if (assembly == null) assembly = SupportedStructures.of(child, StructureGeometry.solid(child), notParent);
            Vec3 allowed = StructureTerrainCollision.clampMotion(child, push, assembly, -1.0D, notParent);
            child.setPos(child.getX() + allowed.x, child.getY() + allowed.y, child.getZ() + allowed.z);
            double inward = child.getDeltaMovement().dot(push.normalize());
            if (inward < 0) child.setDeltaMovement(child.getDeltaMovement().subtract(push.normalize().scale(inward)));
        }
        if (!initial.equals(child.collisionTransform())) {
            StructureCollisionSystem.register(child);
            child.hurtMarked = true;
            CARRIED.put(child, child.level().getGameTime());
            // The clamp above cleared the whole assembly, so its loads follow rigidly. Carrying them
            // level by level instead re-gathered and re-checked the rest of a stack at every level.
            if (assembly != null) carryLoads(assembly, initial, child.collisionTransform());
            // Players and mobs on the child follow now; motion history prevents a second carry in its own tick.
            StructureMotionSystem.tickStructure(child, false);
        }
    }

    private static void carryLoads(SupportedStructures assembly, StructureTransform before, StructureTransform after) {
        float turn = Mth.wrapDegrees(after.yawDegrees() - before.yawDegrees());
        for (CollidableStructure load : assembly.loads()) {
            if (!(load instanceof Entity entity) || entity.isRemoved()) continue;
            StructureTransform from = load.collisionTransform();
            Vec3 to = after.toWorld(before.toLocal(new Vec3(from.x(), from.y(), from.z())));
            entity.setPos(entity.getX() + to.x - from.x(), entity.getY() + to.y - from.y(), entity.getZ() + to.z - from.z());
            if (Math.abs(turn) > 1.0E-6F) load.rotateWithSupport(turn);
            StructureCollisionSystem.register(load);
            entity.hurtMarked = true;
            CARRIED.put(entity, entity.level().getGameTime());
            StructureCollisionSystem.markSupported(entity);
            StructureMotionSystem.tickStructure(load, false);
        }
    }

    private static boolean claimSupport(CollidableStructure child, CollidableStructure parent) {
        synchronized (SUPPORTS) {
            CollidableStructure existing = dereference(SUPPORTS.get(child));
            if (existing != null && existing != parent && stillSupported(child, existing)) return false;
            CollidableStructure ancestor = parent;
            java.util.Set<CollidableStructure> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            while (ancestor != null && visited.add(ancestor)) {
                if (ancestor == child) return false;
                CollidableStructure next = dereference(SUPPORTS.get(ancestor));
                if (next != null && !stillSupported(ancestor, next)) {
                    SUPPORTS.remove(ancestor);
                    next = null;
                }
                ancestor = next;
            }
            SUPPORTS.put(child, new WeakReference<>(parent));
            return true;
        }
    }

    private static boolean stillSupported(CollidableStructure child, CollidableStructure parent) {
        return child instanceof Entity childEntity && parent instanceof Entity parentEntity
                && !childEntity.isRemoved() && !parentEntity.isRemoved() && childEntity.level() == parentEntity.level()
                && childEntity.getDeltaMovement().y <= 1.0E-4D
                && StructureCollisionResolver.supportingGroup(parent.collisionTransform(), StructureGeometry.solid(parent),
                    child.collisionTransform(), StructureGeometry.solid(child), REACH) != null;
    }

    private static CollidableStructure dereference(WeakReference<CollidableStructure> reference) {
        return reference == null ? null : reference.get();
    }
}
