package me.mss1r.axiomata.fabric.mixin;

import me.mss1r.axiomata.fabric.PersistentDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityDataMixin implements PersistentDataHolder {
    @Unique private CompoundTag axiomata$data;

    @Override public CompoundTag axiomata$persistentData() {
        if (axiomata$data == null) axiomata$data = new CompoundTag();
        return axiomata$data;
    }

    @Inject(method = "saveWithoutId", at = @At("HEAD"))
    private void axiomata$saveData(CompoundTag root, CallbackInfoReturnable<CompoundTag> callback) {
        if (axiomata$data != null && !axiomata$data.isEmpty()) root.put(KEY, axiomata$data.copy());
    }

    @Inject(method = "load", at = @At("HEAD"))
    private void axiomata$loadData(CompoundTag root, CallbackInfo callback) {
        axiomata$data = root.contains(KEY, Tag.TAG_COMPOUND) ? root.getCompound(KEY).copy() : null;
    }
}
