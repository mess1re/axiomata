package me.mss1r.axiomata.fabric;

import me.mss1r.axiomata.blueprint.internal.definition.BlueprintDefinitionRepository;
import me.mss1r.axiomata.blueprint.internal.construction.ConstructionMarkupRepository;
import me.mss1r.axiomata.blueprint.internal.construction.ConstructionStateSync;
import me.mss1r.axiomata.blueprint.tracing.OutlineRepository;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.system.StructureCollisionSystem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;

public final class FabricEvents {
    private FabricEvents() {}
    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof CollidableStructure structure) StructureCollisionSystem.register(structure);
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof CollidableStructure structure) StructureCollisionSystem.unregister(structure);
        });
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> ConstructionStateSync.onStartTracking(player, entity));
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
            var players = player.getServer().getPlayerList();
            // Reload sync must validate before the first outgoing catalog, as on Forge.
            if (!joined) BlueprintDefinitionRepository.validate(player.getServer());
            BlueprintDefinitionRepository.syncToClients(player, players);
            OutlineRepository.syncToClients(player, players);
            ConstructionMarkupRepository.syncToClients(player, players);
        });
    }
}
