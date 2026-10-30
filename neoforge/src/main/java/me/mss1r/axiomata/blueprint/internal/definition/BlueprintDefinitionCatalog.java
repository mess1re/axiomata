package me.mss1r.axiomata.blueprint.internal.definition;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
/** Compatibility facade for the loader-facing catalog API. Data loading lives in common. */
public final class BlueprintDefinitionCatalog extends BlueprintDefinitionRepository {
    public static void syncToClients(OnDatapackSyncEvent event) {
        BlueprintDefinitionRepository.syncToClients(event.getPlayer(), event.getPlayerList());
    }
}
