package me.mss1r.axiomata.blueprint.registry;

import me.mss1r.axiomata.blueprint.BlueprintCommon;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.menu.BlueprintUseMenu;
import me.mss1r.axiomata.blueprint.menu.DrawingTableMenu;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.InteractionHand;

public class BlueprintMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BlueprintCommon.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<DrawingTableMenu>> DRAWING_TABLE_MENU = MENUS.register("drawing_table",
            () -> MenuRegistry.ofExtended((id, inv, data) -> new DrawingTableMenu(id, inv, data.readBlockPos())));

    public static final RegistrySupplier<MenuType<BlueprintUseMenu>> BLUEPRINT_USE_MENU = MENUS.register("blueprint_use",
            () -> MenuRegistry.ofExtended((id, inv, data) -> {
                String recipeId = data.readUtf();
                BlueprintDefinition recipe = BlueprintDefinitions.get(recipeId);
                InteractionHand hand = data.readEnum(InteractionHand.class);
                return new BlueprintUseMenu(id, inv, recipe, inv.player.getItemInHand(hand), recipeId, hand);
            }));

    public static void register() {
        MENUS.register();
    }
}
