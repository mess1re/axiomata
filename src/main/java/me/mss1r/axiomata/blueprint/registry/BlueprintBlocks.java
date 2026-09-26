package me.mss1r.axiomata.blueprint.registry;

import me.mss1r.axiomata.blueprint.BlueprintModule;
import me.mss1r.axiomata.blueprint.block.DrawingTableBlock;
import me.mss1r.axiomata.blueprint.block.InkBlock;
import me.mss1r.axiomata.blueprint.block.Inkwell;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

public class BlueprintBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BlueprintModule.MOD_ID, Registries.BLOCK);

    public static final RegistrySupplier<DrawingTableBlock> DRAWING_TABLE =
            BLOCKS.register("drawing_table",
                    () -> new DrawingTableBlock(Block.Properties.of().strength(2.0F).noOcclusion()));

    public static final RegistrySupplier<InkBlock> INK_BLOCK =
            BLOCKS.register("ink_block", InkBlock::new);

    public static final RegistrySupplier<Inkwell> INKWELL =
            BLOCKS.register("inkwell", Inkwell::new);

    public static void register() {
        BLOCKS.register();
    }
}
