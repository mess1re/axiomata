package me.mss1r.axiomata.neoforge.mixin;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.system.StructureCollisionSystem;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class EntityLifecycleMixin {
    // Register after a successful level addition, including disk loads of motionless sites.
    // A pre-add event is cancellable and could leave an entity that never spawned in the index.
    @Inject(method =
            "onAddedToLevel",
            at = @At("TAIL"), remap = false)
    private void axiomata$registerAddedStructure(CallbackInfo callbackInfo) {
        if ((Object) this instanceof CollidableStructure structure) StructureCollisionSystem.register(structure);
    }

    @Inject(method =
            "onRemovedFromLevel",
            at = @At("TAIL"), remap = false)
    private void axiomata$unregisterRemovedStructure(CallbackInfo callbackInfo) {
        if ((Object) this instanceof CollidableStructure structure) StructureCollisionSystem.unregister(structure);
    }

}
