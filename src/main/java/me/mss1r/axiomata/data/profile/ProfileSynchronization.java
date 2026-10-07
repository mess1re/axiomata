package me.mss1r.axiomata.data.profile;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.utils.GameInstance;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class ProfileSynchronization {
    private ProfileSynchronization() {}

    /** Call once during mod initialization. The packet factory reads the catalog's current snapshot. */
    public static <P> void register(ProfileCatalog<?> catalog, Supplier<P> packet,
                                    BiConsumer<ServerPlayer, P> send) {
        PlayerEvent.PLAYER_JOIN.register(player -> send.accept(player, packet.get()));
        catalog.onPublish(() -> {
            var server = GameInstance.getServer();
            if (server == null) {
                return;
            }
            P payload = packet.get();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                send.accept(player, payload);
            }
        });
    }
}
