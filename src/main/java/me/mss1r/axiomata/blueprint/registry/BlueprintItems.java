package me.mss1r.axiomata.blueprint.registry;

import me.mss1r.axiomata.blueprint.BlueprintModule;
import me.mss1r.axiomata.blueprint.item.BlueprintItem;
import me.mss1r.axiomata.blueprint.item.ConstructionHammerItem;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class BlueprintItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BlueprintModule.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> DRAWING_TABLE = ITEMS.register("drawing_table",
            () -> new BlockItem(BlueprintBlocks.DRAWING_TABLE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> INK_BLOCK = ITEMS.register("ink_block",
            () -> new BlockItem(BlueprintBlocks.INK_BLOCK.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> INKWELL = ITEMS.register("inkwell",
            () -> new BlockItem(BlueprintBlocks.INKWELL.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> BLUEPRINT = ITEMS.register("blueprint", BlueprintItem::new);
    public static final RegistrySupplier<Item> CONSTRUCTION_HAMMER = ITEMS.register("construction_hammer",
            () -> new ConstructionHammerItem(new Item.Properties().stacksTo(1).durability(2048)));

    public static void register() {
        ITEMS.register();
    }
}
