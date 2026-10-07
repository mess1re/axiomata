package me.mss1r.axiomata.mixin;

import me.mss1r.axiomata.ballistics.BallisticProjectile;
import me.mss1r.axiomata.ballistics.ProjectileSweep;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(ProjectileUtil.class)
public abstract class ProjectileUtilMixin {
    @Inject(
            method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;"
                    + "Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;"
                    + "Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)"
                    + "Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void axiomata$traceBallisticProjectilesAgainstModelGeometry(
            Level level, Entity projectile, Vec3 start, Vec3 end, AABB searchBox,
            Predicate<Entity> targetPredicate, float hitboxPadding,
            CallbackInfoReturnable<EntityHitResult> callback) {
        if (!(level instanceof ServerLevel serverLevel) || !(projectile instanceof BallisticProjectile)) {
            return;
        }

        EntityHitResult hit = ProjectileSweep.findFirstEntityHit(
                serverLevel, projectile, start, end, end.subtract(start), targetPredicate, hitboxPadding);
        callback.setReturnValue(hit);
    }
}
