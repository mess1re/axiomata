package me.mss1r.axiomata.collision.system;

import me.mss1r.axiomata.collision.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Predicate;

/** Transient contact graph, rebuilt from geometry. There are no saved or mounted attachments. */
final class SupportedStructures {
    private final Set<CollidableStructure> members = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<CollisionGroup> groups = new ArrayList<>();
    private final List<CollidableStructure> loads = new ArrayList<>();
    private final StructureTransform root;
    private final Entity rootEntity;
    private final Predicate<CollidableStructure> ignored;

    private SupportedStructures(Entity entity, CollidableStructure structure, List<CollisionGroup> rootGroups,
                                Predicate<CollidableStructure> ignored) {
        this.root = structure.collisionTransform();
        this.rootEntity = entity;
        this.ignored = ignored;
        members.add(structure);
        groups.addAll(rootGroups);
        collect(structure);
    }

    static <T extends Entity & CollidableStructure> SupportedStructures of(
            T root, List<CollisionGroup> groups, Predicate<CollidableStructure> ignored) {
        return new SupportedStructures(root, root, groups, ignored);
    }

    List<CollisionGroup> groups() { return groups; }
    boolean contains(CollidableStructure structure) { return members.contains(structure); }
    /** Everything riding on the root, directly or on other loads; the root itself is not listed. */
    List<CollidableStructure> loads() { return loads; }

    private void collect(CollidableStructure parent) {
        AABB bounds = StructureCollisionResolver.worldBounds(parent, StructureGeometry.solid(parent));
        if (bounds == null) return;
        for (CollidableStructure candidate : StructureCollisionRegistry.structuresNear(rootEntity, bounds.inflate(.03D), .03D)) {
            if (members.contains(candidate) || ignored.test(candidate) || !(candidate instanceof Entity entity)
                    || !entity.isAlive() || entity.noPhysics || entity.isPassenger() || entity.getDeltaMovement().y > 1.0E-4D) continue;
            if (StructureCollisionResolver.supportingGroup(parent.collisionTransform(), StructureGeometry.solid(parent),
                    candidate.collisionTransform(), StructureGeometry.supportContact(candidate), .03D) == null) continue;
            if (StructureTerrainCollision.isSupportedByWorld((Entity & CollidableStructure) entity, .03D)) continue;
            members.add(candidate);
            loads.add(candidate);
            StructureTransform transform = candidate.collisionTransform();
            Rotation3 rotation = Rotation3.aroundY((float) Math.toRadians(root.yawDegrees() - transform.yawDegrees()));
            CollisionPose relative = new CollisionPose(rotation,
                    root.toLocal(new Vec3(transform.x(), transform.y(), transform.z())));
            int index = members.size();
            for (CollisionGroup group : StructureGeometry.solid(candidate)) {
                groups.add(new CollisionGroup("support/" + index + "/" + group.name(), group.shape(), group.pose().then(relative)));
            }
            collect(candidate);
        }
    }
}
