package me.mss1r.axiomata.blueprint.client.events;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import me.mss1r.axiomata.Axiomata;
import me.mss1r.axiomata.blueprint.client.renderer.ConstructionHighlightRenderType;
import me.mss1r.axiomata.blueprint.internal.construction.BuildSectionCatalog;
import me.mss1r.axiomata.blueprint.registry.BlueprintBlocks;
import me.mss1r.axiomata.blueprint.client.screen.BlueprintUseScreen;
import me.mss1r.axiomata.blueprint.registry.BlueprintMenus;
import me.mss1r.axiomata.blueprint.client.screen.DrawingTableScreen;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
//? if forge {
/*import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
*///?} else {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
//?}

import java.io.IOException;
import java.io.UncheckedIOException;

public class BlueprintClientModEvents {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(BlueprintBlocks.DRAWING_TABLE.get(), RenderType.cutout());
            MenuRegistry.registerScreenFactory(BlueprintMenus.DRAWING_TABLE_MENU.get(), DrawingTableScreen::new);
            MenuRegistry.registerScreenFactory(BlueprintMenus.BLUEPRINT_USE_MENU.get(), BlueprintUseScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new BuildSectionCatalog());
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            ResourceLocation.fromNamespaceAndPath(Axiomata.MOD_ID, "construction_highlight"),
                            DefaultVertexFormat.NEW_ENTITY
                    ),
                    ConstructionHighlightRenderType::setShader
            );
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not load the construction highlight shader", exception);
        }
    }
}
