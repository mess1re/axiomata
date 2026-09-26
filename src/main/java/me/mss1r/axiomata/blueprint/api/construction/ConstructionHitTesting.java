package me.mss1r.axiomata.blueprint.api.construction;

import me.mss1r.axiomata.blueprint.api.visual.BlueprintConstructionVisuals;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.structure.SectionBounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class ConstructionHitTesting {
    private static final double DEFAULT_MARGIN_PIXELS = 8.0D;
    private static final double DEFAULT_COLLISION_PADDING = 0.25D;
    private static final double DEFAULT_REACH_BONUS = 1.0D;

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
        SectionBounds bounds = BlueprintConstructionVisuals.bounds(model);
        SectionBounds.Section target = bounds == null ? null : bounds.find(section).orElse(null);
        if (target == null) {
            return true;
        }

        Vec3 start = builder.getEyePosition();
        Vec3 end = start.add(builder.getViewVector(1.0F).scale(
                interactionRange(builder) + Math.max(0.0D, reachBonus)));
        Vec3 hit = StructureCollisionResolver
                .clipSegment(structure, start, end, Math.max(0.0D, collisionPadding))
                .orElse(null);
        if (hit == null) {
            return true;
        }

        Vec3 local = structure.collisionTransform().toLocal(hit);
        return target.bounds().contains(
                -local.x * 16.0D,
                local.y * 16.0D,
                local.z * 16.0D,
                Math.max(0.0D, marginPixels));
    }

    private static double interactionRange(Player player) {
        //? if forge {
        /*return player.getEntityReach();
        *///?} else {
        return player.entityInteractionRange();
        //?}
    }
}
