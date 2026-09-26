package me.mss1r.axiomata.blueprint.registry;

import me.mss1r.axiomata.blueprint.BlueprintModule;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class BlueprintCreativeTab {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(BlueprintModule.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> MAIN = TABS.register("axiomata",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(BlueprintItems.DRAWING_TABLE.get()))
                    .title(Component.translatable("itemGroup.axiomata"))
                    .displayItems((params, output) -> {
                        output.accept(BlueprintItems.DRAWING_TABLE.get());
                        output.accept(BlueprintItems.INK_BLOCK.get());
                        output.accept(BlueprintItems.INKWELL.get());
                        output.accept(BlueprintItems.BLUEPRINT.get());
                        output.accept(BlueprintItems.CONSTRUCTION_HAMMER.get());
                    }).build()
    );

    public static void register() {
        TABS.register();
    }
}
