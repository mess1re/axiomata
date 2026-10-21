package me.mss1r.axiomata.mixin;

import me.mss1r.axiomata.blueprint.api.construction.ConstructionWork;
import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.system.StructureCollisionSystem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * What every result does while it is built, so that its mod need not: it carries no one, stays where it was placed,
 * keeps its progress in its own save data and leaves debris when it is destroyed.
 */
@Mixin(Entity.class)
public abstract class UnderConstructionMixin {
    private static final String PROGRESS_TAG = "AxiomataConstruction";

    private boolean axiomata$unfinished() {
        return this instanceof UnderConstruction machine && !machine.isFullyBuilt();
    }

    @Inject(method = "canAddPassenger", at = @At("HEAD"), cancellable = true)
    private void axiomata$noRidersWhileUnfinished(Entity passenger, CallbackInfoReturnable<Boolean> callback) {
        if (axiomata$unfinished()) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void axiomata$standStillWhileUnfinished(MoverType type, Vec3 movement, CallbackInfo callback) {
        if (axiomata$unfinished()) {
            Entity self = (Entity) (Object) this;
            // No velocity may build up either, or the finished result would leap off.
            self.setDeltaMovement(Vec3.ZERO);
            if (self instanceof CollidableStructure structure) {
                StructureCollisionSystem.register(structure);
            }
            callback.cancel();
        }
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void axiomata$saveProgress(CompoundTag tag, CallbackInfoReturnable<CompoundTag> callback) {
        if (this instanceof UnderConstruction machine && !machine.buildProgress().blueprintId().isEmpty()) {
            CompoundTag progress = new CompoundTag();
            machine.buildProgress().save(progress);
            callback.getReturnValue().put(PROGRESS_TAG, progress);
        }
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void axiomata$loadProgress(CompoundTag tag, CallbackInfo callback) {
        if (this instanceof UnderConstruction machine && tag.contains(PROGRESS_TAG)) {
            machine.buildProgress().load(tag.getCompound(PROGRESS_TAG));
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void axiomata$leaveDebris(Entity.RemovalReason reason, CallbackInfo callback) {
        Entity self = (Entity) (Object) this;
        if (reason == Entity.RemovalReason.KILLED && !self.level().isClientSide() && !self.isRemoved()
                && this instanceof UnderConstruction machine && !machine.isFullyBuilt()) {
            for (ItemStack stack : ConstructionWork.debris(machine)) {
                self.spawnAtLocation(stack);
            }
        }
    }
}
