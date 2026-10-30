package me.mss1r.axiomata.blueprint.internal.construction;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
/** Compatibility facade for the loader-facing catalog API. Data loading lives in common. */
public final class ConstructionMarkupCatalog extends ConstructionMarkupRepository {
    public static void syncToClients(OnDatapackSyncEvent event) {
        ConstructionMarkupRepository.syncToClients(event.getPlayer(), event.getPlayerList());
    }
}
