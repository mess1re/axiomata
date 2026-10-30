package me.mss1r.axiomata.blueprint;

import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.blueprint.command.BlueprintCommands;
import me.mss1r.axiomata.blueprint.tracing.OutlineRepository;
import me.mss1r.axiomata.blueprint.internal.construction.HammerWork;
import me.mss1r.axiomata.blueprint.internal.construction.ConstructionMarkupRepository;
import me.mss1r.axiomata.blueprint.internal.definition.BlueprintDefinitionRepository;
import me.mss1r.axiomata.blueprint.config.BlueprintServerConfig;
import me.mss1r.axiomata.blueprint.network.NetworkHandler;
import me.mss1r.axiomata.blueprint.registry.*;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
public final class BlueprintCommon {
    public static final String MOD_ID = "axiomata";
    public static void initialize() {
        BlueprintBlocks.register();
        BlueprintItems.register();
        BlueprintSounds.register();
        BlueprintBlockEntities.register();
        BlueprintEntityTypes.register();
        BlueprintMenus.register();
        BlueprintCreativeTab.register();
        NetworkHandler.register();
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new BlueprintDefinitionRepository(),
                ResourceIds.id(MOD_ID, "blueprints"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new OutlineRepository(),
                ResourceIds.id(MOD_ID, "blueprint_outlines"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new ConstructionMarkupRepository(),
                ResourceIds.id(MOD_ID, "construction_markup"));
        InteractionEvent.INTERACT_ENTITY.register(HammerWork::onEntityInteract);
        CommandRegistrationEvent.EVENT.register(BlueprintCommands::register);
        LifecycleEvent.SERVER_STARTED.register(BlueprintDefinitionRepository::validate);
    }
}
