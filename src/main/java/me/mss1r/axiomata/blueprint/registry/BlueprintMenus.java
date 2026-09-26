package me.mss1r.axiomata.blueprint.registry;

import me.mss1r.axiomata.blueprint.BlueprintModule;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.menu.BlueprintUseMenu;
import me.mss1r.axiomata.blueprint.menu.DrawingTableMenu;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class BlueprintMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BlueprintModule.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<DrawingTableMenu>> DRAWING_TABLE_MENU = MENUS.register("drawing_table",
            () -> MenuRegistry.ofExtended((id, inv, data) -> new DrawingTableMenu(id, inv, data.readBlockPos())));

    public static final RegistrySupplier<MenuType<BlueprintUseMenu>> BLUEPRINT_USE_MENU = MENUS.register("blueprint_use",
            () -> MenuRegistry.ofExtended((id, inv, data) -> {
                String recipeId = data.readUtf();
                BlueprintDefinition recipe = BlueprintDefinitions.get(recipeId);
                ItemStack blueprint = inv.player.getMainHandItem().getItem() == BlueprintItems.BLUEPRINT.get()
                        ? inv.player.getMainHandItem() : inv.player.getOffhandItem();
                return new BlueprintUseMenu(id, inv, recipe, blueprint, recipeId);
            }));

    public static void register() {
        MENUS.register();
    }
}
