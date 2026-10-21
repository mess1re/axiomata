package me.mss1r.axiomata.blueprint.api.construction;

import me.mss1r.axiomata.blueprint.api.visual.BlueprintConstructionVisuals;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.StructureTransform;
import me.mss1r.axiomata.structure.SectionBounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Optional;

public final class ConstructionHitTesting {
    private static final double DEFAULT_MARGIN_PIXELS = 0.5D;
    private static final double DEFAULT_COLLISION_PADDING = 0.0D;
    private static final double DEFAULT_REACH_BONUS = 0.0D;

    private ConstructionHitTesting() {
    }

    public static boolean accepts(Player builder, CollidableStructure structure,
                                  ResourceLocation model, String section) {
        return accepts(
                builder, structure, model, section,
                DEFAULT_REACH_BONUS, DEFAULT_COLLISION_PADDING, DEFAULT_MARGIN_PIXELS);
    }

    public static boolean accepts(Player builder, CollidableStructure structure,
                                  ResourceLocation model, String section,
                                  double reachBonus, double collisionPadding, double marginPixels) {
        return accepts(builder, structure.collisionTransform(), structure, model, section,
                reachBonus, collisionPadding, marginPixels);
    }

    /** For a result without collision geometry: its markup is placed by the entity's position and yaw. */
    public static boolean accepts(Player builder, Entity result, ResourceLocation model, String section) {
        StructureTransform transform = result instanceof CollidableStructure structure
                ? structure.collisionTransform()
                : new StructureTransform(result.getX(), result.getY(), result.getZ(), result.getYRot());
        return accepts(builder, transform, result, model, section,
                DEFAULT_REACH_BONUS, DEFAULT_COLLISION_PADDING, DEFAULT_MARGIN_PIXELS);
    }

    private static boolean accepts(Player builder, StructureTransform transform, Object result,
                                   ResourceLocation model, String section,
                                   double reachBonus, double collisionPadding, double marginPixels) {
        SectionBounds bounds = BlueprintConstructionVisuals.bounds(model);
        SectionBounds.Section target = bounds == null ? null : bounds.find(section).orElse(null);
        if (bounds == null) {
            return true;
        }
        if (target == null) {
            return false;
        }

        Vec3 start = builder.getEyePosition();
        double reach = interactionRange(builder) + Math.max(0.0D, reachBonus);
        Vec3 end = start.add(builder.getViewVector(1.0F).scale(reach));
        HitResult block = builder.pick(reach, 1.0F, false);
        if (block.getType() == HitResult.Type.BLOCK) {
            end = block.getLocation();
        }

        Set<String> visible = new LinkedHashSet<>();
        visible.add(section);
        if (result instanceof UnderConstruction machine) {
            visible.addAll(machine.constructionVisualState().builtSections());
        }
        return bounds.firstHit(transform.toLocal(start), transform.toLocal(end), visible,
                Math.max(0, marginPixels) + Math.max(0, collisionPadding) * 16)
                .map(hit -> hit.name().equals(section)).orElse(false);
    }

    /** Pick built sections and the active section; later ghost sections do not block the cursor. */
    public static Optional<Vec3> clip(CollidableStructure structure, UnderConstruction machine, Vec3 start, Vec3 end) {
        ResourceLocation model = machine.constructionModel();
        SectionBounds bounds = model == null ? null : BlueprintConstructionVisuals.bounds(model);
        if (bounds == null) {
            return StructureCollisionResolver.clipSegment(structure, start, end);
        }
        var state = machine.constructionVisualState();
        Set<String> visible = new LinkedHashSet<>(state.builtSections());
        visible.add(state.activeSection());
        var transform = structure.collisionTransform();
        Vec3 from = transform.toLocal(start);
        Vec3 to = transform.toLocal(end);
        return bounds.firstHit(from, to, visible, DEFAULT_MARGIN_PIXELS)
                .map(hit -> start.lerp(end, Math.sqrt(hit.distance())));
    }

    private static double interactionRange(Player player) {
        //? if forge {
        /*return player.getEntityReach();
        *///?} else {
        return player.entityInteractionRange();
        //?}
    }
}
