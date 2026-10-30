package me.mss1r.axiomata.fabric;
import net.minecraft.nbt.CompoundTag;
/** Persistent artillery state attached to vanilla entities on Fabric. */
public interface PersistentDataHolder {
    String KEY = "axiomata:entity_data";
    CompoundTag axiomata$persistentData();
}
