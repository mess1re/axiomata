package me.mss1r.axiomata.blueprint.internal.construction;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;
import me.mss1r.axiomata.blueprint.api.construction.ConstructionStatus;
/** Retains native event entry points; state and transmission live in common. */
public final class ConstructionSync {
    private ConstructionSync() {}
    public static void changed(UnderConstruction machine) { ConstructionStateSync.changed(machine); }
    public static ConstructionStatus status(UnderConstruction machine) { return ConstructionStateSync.status(machine); }
    public static void receive(Entity entity, ConstructionStatus status) { ConstructionStateSync.receive(entity, status); }
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player) ConstructionStateSync.onStartTracking(player, event.getTarget());
    }
}
