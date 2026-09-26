package me.mss1r.axiomata.blueprint.registry;

import me.mss1r.axiomata.blueprint.BlueprintModule;
import me.mss1r.axiomata.blueprint.blockentity.DrawingTableBlockEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BlueprintBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BlueprintModule.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<DrawingTableBlockEntity>> DRAWING_TABLE = BLOCK_ENTITIES.register("drawing_table",
            () -> BlockEntityType.Builder.of(DrawingTableBlockEntity::new, BlueprintBlocks.DRAWING_TABLE.get()).build(null));

    public static void register() {
        BLOCK_ENTITIES.register();
    }
}
