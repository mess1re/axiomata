package me.mss1r.axiomata.ballistics;

import me.mss1r.axiomata.platform.AxiomataPlatform;
import me.mss1r.axiomata.ResourceIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
public final class ProjectileCombat {
    private ProjectileCombat() {}

    public static final ResourceKey<DamageType> PHYSICAL_PROJECTILE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceIds.id("axiomata", "physical_projectile"));
    public static final ResourceKey<DamageType> PHYSICAL_BLAST = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceIds.id("axiomata", "physical_blast"));
    public static final ResourceKey<DamageType> PHYSICAL_IMPACT = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceIds.id("axiomata", "physical_impact"));

    /** Armor was resolved physically before hurt(); enchantments, immunity and damage events still run normally. */
    public static DamageSource source(Level level, ResourceKey<DamageType> type, @Nullable Entity direct, @Nullable Entity owner) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), direct, owner);
    }

    @Nullable
    public static Entity logicalTarget(@Nullable Entity target) {
        return AxiomataPlatform.logicalTarget(target);
    }

    @Nullable
    public static LivingEntity livingTarget(Entity target) {
        Entity logicalTarget = logicalTarget(target);
        return logicalTarget instanceof LivingEntity living ? living : null;
    }

    public static int targetId(Entity target) {
        return logicalTarget(target).getId();
    }

    public static void breakBlockingShield(LivingEntity target) {
        if (!target.isBlocking()) {
            return;
        }

        ItemStack activeItem = target.getUseItem();
        if (!AxiomataPlatform.blocksWithShield(activeItem)) {
            return;
        }

        //? if >=1.21 {
        target.onEquippedItemBroken(activeItem.getItem(), LivingEntity.getSlotForHand(target.getUsedItemHand()));
        //?}
        activeItem.shrink(1);
        //? if <1.21 {
        /*target.broadcastBreakEvent(target.getUsedItemHand());
        *///?}
        target.stopUsingItem();
    }
}
