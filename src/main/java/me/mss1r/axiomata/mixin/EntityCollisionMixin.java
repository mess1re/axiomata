package me.mss1r.axiomata.mixin;

import me.mss1r.axiomata.collision.system.StructureCollisionSystem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityCollisionMixin {
    @Unique
    private boolean axiomata$supportedByStructure;

    @Inject(method = "move", at = @At("HEAD"))
    private void axiomata$resetStructureSupport(MoverType moverType, Vec3 movement, CallbackInfo callbackInfo) {
        axiomata$supportedByStructure = false;
    }

    @Inject(method = "collide", at = @At("RETURN"), cancellable = true)
    private void axiomata$collideWithStructures(Vec3 requested,
                                                CallbackInfoReturnable<Vec3> callbackInfo) {
        StructureCollisionSystem.Movement collision = StructureCollisionSystem.collideMovement(
                (Entity) (Object) this, requested, callbackInfo.getReturnValue());
        axiomata$supportedByStructure |= collision.supported();
        callbackInfo.setReturnValue(collision.allowed());
    }

    @Inject(method = "move", at = @At("TAIL"))
    private void axiomata$applyStructureSupport(MoverType moverType, Vec3 movement, CallbackInfo callbackInfo) {
        if (axiomata$supportedByStructure) {
            StructureCollisionSystem.markSupported((Entity) (Object) this);
        }
    }
}
