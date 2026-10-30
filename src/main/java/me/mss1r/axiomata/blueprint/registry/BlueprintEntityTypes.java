package me.mss1r.axiomata.blueprint.registry;

import me.mss1r.axiomata.blueprint.BlueprintCommon;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;

public class BlueprintEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BlueprintCommon.MOD_ID, Registries.ENTITY_TYPE);

    public static void register() {
        ENTITY_TYPES.register();
    }
}
