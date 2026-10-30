package me.mss1r.axiomata.blueprint.internal.construction;

import com.google.common.collect.MapMaker;
import me.mss1r.axiomata.blueprint.api.construction.ConstructionStatus;
import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;
import me.mss1r.axiomata.blueprint.network.NetworkHandler;
import me.mss1r.axiomata.blueprint.network.S2CConstructionStatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import java.util.Map;

/**
 * Keeps clients' view of every build current, so a result only stores its {@link
 * me.mss1r.axiomata.blueprint.api.construction.BuildProgress}. Players receive the state when the entity comes into
 * view and again on every change.
 */
public final class ConstructionStateSync {
    private static final Map<Entity, ConstructionStatus> RECEIVED = new MapMaker().weakKeys().makeMap();

    private ConstructionStateSync() {
    }

    public static void changed(UnderConstruction machine) {
        if (machine instanceof Entity entity && !entity.level().isClientSide()) {
            NetworkHandler.sendToTracking(entity, packet(entity, machine));
        }
    }

    public static void onStartTracking(ServerPlayer player, Entity target) {
        if (target instanceof UnderConstruction machine) {
            NetworkHandler.sendToPlayer(player, packet(target, machine));
        }
    }

    /** The server's own state, or what this client last received. */
    public static ConstructionStatus status(UnderConstruction machine) {
        if (machine instanceof Entity entity && entity.level().isClientSide()) {
            return RECEIVED.getOrDefault(entity, ConstructionStatus.FINISHED);
        }
        return ConstructionStatus.of(machine.buildProgress());
    }

    public static void receive(Entity entity, ConstructionStatus status) {
        if (entity instanceof UnderConstruction) {
            RECEIVED.put(entity, status);
        }
    }

    private static S2CConstructionStatePacket packet(Entity entity, UnderConstruction machine) {
        return new S2CConstructionStatePacket(entity.getId(), ConstructionStatus.of(machine.buildProgress()));
    }
}
