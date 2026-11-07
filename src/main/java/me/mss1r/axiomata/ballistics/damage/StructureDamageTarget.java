package me.mss1r.axiomata.ballistics.damage;

import net.minecraft.resources.ResourceLocation;

/** Optional profile selection for variants of the same entity type. */
public interface StructureDamageTarget {
    ResourceLocation structureDamageProfileId();
}
