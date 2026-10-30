package me.mss1r.axiomata.fabric.mixin;

import me.mss1r.axiomata.ballistics.ProjectilePassThroughControl;
import me.mss1r.axiomata.ballistics.ProjectilePassageHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.util.function.Predicate;

/** Fabric has no Forge impact event: skip openings during the actual entity sweep. */
@Mixin(ProjectileUtil.class)
public abstract class ProjectileOpeningsMixin {
    @ModifyVariable(method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;"
            + "Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;"
            + "Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Predicate<Entity> axiomata$skipOpenings(Predicate<Entity> original,
            Level level, Entity projectile, Vec3 start, Vec3 end, AABB area, Predicate<Entity> ignored, float padding) {
        if (!(projectile instanceof Projectile)) return original;
        return candidate -> original.test(candidate)
                && (!(candidate instanceof ProjectilePassThroughControl opening)
                    || !ProjectilePassageHandler.allowsPassage(opening, start, end.subtract(start), end));
    }
}
