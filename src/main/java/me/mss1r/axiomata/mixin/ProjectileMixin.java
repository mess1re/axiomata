package me.mss1r.axiomata.mixin;

import me.mss1r.axiomata.ballistics.ProjectilePassThroughControl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Projectile.class)
public abstract class ProjectileMixin {
    @Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
    private void axiomata$usePassThroughGeometryForVehicle(Entity target,
                                                              CallbackInfoReturnable<Boolean> callback) {
        if (target instanceof ProjectilePassThroughControl && target.canBeHitByProjectile()) {
            callback.setReturnValue(true);
        }
    }
}
