package me.mss1r.axiomata.blueprint.tracing;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
/** Compatibility facade for the loader-facing catalog API. Data loading lives in common. */
public final class OutlineCatalog extends OutlineRepository {
    public static void syncToClients(OnDatapackSyncEvent event) {
        OutlineRepository.syncToClients(event.getPlayer(), event.getPlayerList());
    }
}
