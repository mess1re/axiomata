package me.mss1r.axiomata.blueprint;

import me.mss1r.axiomata.blueprint.command.BlueprintCommands;
import me.mss1r.axiomata.blueprint.tracing.OutlineCatalog;
import me.mss1r.axiomata.blueprint.internal.construction.HammerWork;
import me.mss1r.axiomata.blueprint.internal.construction.SectionBoundsCatalog;
import me.mss1r.axiomata.blueprint.internal.definition.BlueprintDefinitionCatalog;
import me.mss1r.axiomata.blueprint.config.BlueprintServerConfig;
import me.mss1r.axiomata.blueprint.network.NetworkHandler;
import me.mss1r.axiomata.blueprint.registry.*;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
*///?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
//?}

public final class BlueprintModule {
    public static final String MOD_ID = "axiomata";
    //? if forge {
    /*public static void initialize(IEventBus modEventBus, IEventBus gameEventBus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, BlueprintServerConfig.SPEC);
        initializeCommon(modEventBus, gameEventBus);
    }
    *///?} else {
    public static void initialize(IEventBus modEventBus, ModContainer modContainer, IEventBus gameEventBus) {
        modContainer.registerConfig(ModConfig.Type.SERVER, BlueprintServerConfig.SPEC);
        initializeCommon(modEventBus, gameEventBus);
    }
    //?}

    private static void initializeCommon(IEventBus modEventBus, IEventBus gameEventBus) {
        BlueprintBlocks.register();
        BlueprintItems.register();
        BlueprintBlockEntities.register();
        BlueprintEntityTypes.register();
        BlueprintMenus.register();
        BlueprintCreativeTab.register();
        NetworkHandler.register();
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new BlueprintDefinitionCatalog(),
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "blueprints"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new OutlineCatalog(),
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "blueprint_outlines"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new SectionBoundsCatalog(),
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "construction_bounds"));
        InteractionEvent.INTERACT_ENTITY.register(HammerWork::onEntityInteract);
        CommandRegistrationEvent.EVENT.register(BlueprintCommands::register);
        gameEventBus.addListener(BlueprintDefinitionCatalog::syncToClients);
        gameEventBus.addListener(OutlineCatalog::syncToClients);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(me.mss1r.axiomata.blueprint.client.events.BlueprintClientModEvents::clientSetup);
            modEventBus.addListener(me.mss1r.axiomata.blueprint.client.events.BlueprintClientModEvents::registerReloadListeners);
            modEventBus.addListener(me.mss1r.axiomata.blueprint.client.events.BlueprintClientModEvents::registerShaders);
            gameEventBus.addListener(me.mss1r.axiomata.blueprint.client.events.BlueprintClientGameEvents::onRenderLevelStage);
        }
    }

}
