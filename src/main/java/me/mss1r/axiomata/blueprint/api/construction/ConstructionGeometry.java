package me.mss1r.axiomata.blueprint.api.construction;

import com.google.common.collect.MapMaker;
import me.mss1r.axiomata.blueprint.api.visual.BlueprintConstructionVisuals;
import me.mss1r.axiomata.collision.ClimbableGroup;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPart;
import me.mss1r.axiomata.collision.CollisionPose;
import me.mss1r.axiomata.collision.CollisionShape;
import me.mss1r.axiomata.collision.OrientedBox;
import me.mss1r.axiomata.structure.SectionBounds;
import me.mss1r.axiomata.structure.StructureSections;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What of an unfinished build already stands. Builders walk on its built sections and climb its built ladders while
 * it rises, as they would on a real frame; the sections still to come stay out of the way.
 */
public final class ConstructionGeometry {
    // Keyed by the loaded markup, so a reload starts over; the lists stay the same objects while the build does not
    // change, which keeps the collision caches warm.
    private static final Map<SectionBounds, Map<Set<String>, List<CollisionGroup>>> GROUPS =
            new MapMaker().weakKeys().makeMap();

    private ConstructionGeometry() {
    }

    /**
     * Solid geometry of the sections built so far, one group per section named after it, from the cubes in the
     * construction markup. Return it from {@code solidCollisionGroups()} while the result is unfinished.
     */
    public static List<CollisionGroup> builtGroups(UnderConstruction machine) {
        ResourceLocation model = machine.constructionModel();
        SectionBounds bounds = model == null ? null : BlueprintConstructionVisuals.bounds(model);
        if (bounds == null) {
            return List.of();
        }
        Set<String> built = Set.copyOf(machine.constructionVisualState().builtSections());
        return GROUPS.computeIfAbsent(bounds, key -> new ConcurrentHashMap<>())
                .computeIfAbsent(built, key -> groups(bounds, key));
    }

    /** The climbables whose bone already stands; return them from {@code climbableGroups()} while unfinished. */
    public static List<ClimbableGroup> builtClimbables(UnderConstruction machine, List<ClimbableGroup> climbables) {
        return climbables.stream().filter(climbable -> isBoneBuilt(machine, climbable.collision().name())).toList();
    }

    /**
     * Whether a bone of the model stands: every section holding its cubes is built. A bone the markup lists with no
     * cubes, such as a rope anchor, belongs to that section. A bone in no section appears with the finished result.
     */
    public static boolean isBoneBuilt(UnderConstruction machine, String bone) {
        ResourceLocation model = machine.constructionModel();
        return isBoneBuilt(model == null ? null : BlueprintConstructionVisuals.sections(model),
                machine.constructionVisualState(), bone);
    }

    public static boolean isBoneBuilt(StructureSections sections, BlueprintConstructionVisuals.State state,
                                      String bone) {
        if (state.activeStage() >= state.stageCount()) {
            return true;
        }
        if (sections == null) {
            return false;
        }
        boolean found = false;
        for (StructureSections.Section section : sections.sections()) {
            if (section.parts().containsKey(bone)) {
                found = true;
                if (!state.builtSections().contains(section.name())) {
                    return false;
                }
            }
        }
        return found;
    }

    /** {@link #builtGroups(UnderConstruction)} for markup and built sections at hand. */
    public static List<CollisionGroup> groups(SectionBounds bounds, Set<String> built) {
        List<CollisionGroup> groups = new ArrayList<>();
        for (SectionBounds.Section section : bounds.sections()) {
            if (!built.contains(section.name()) || section.parts().isEmpty()) {
                continue;
            }
            List<CollisionPart> parts = new ArrayList<>(section.parts().size());
            for (OrientedBox part : section.parts()) {
                Vec3 half = part.halfExtent();
                parts.add(new CollisionPart(new AABB(-half.x, -half.y, -half.z, half.x, half.y, half.z),
                        new CollisionPose(part.rotation(), part.center())));
            }
            groups.add(CollisionGroup.fixed(section.name(), new CollisionShape(Vec3.ZERO, parts)));
        }
        return List.copyOf(groups);
    }
}
