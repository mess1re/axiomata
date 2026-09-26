package me.mss1r.axiomata.collision.system;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.WeakHashMap;

public final class VirtualPlatformSupport {
    private static final long SUPPORT_GRACE_TICKS = 1L;
    private static final Map<ServerPlayer, Long> SUPPORTED_PLAYERS = new WeakHashMap<>();

    private VirtualPlatformSupport() {
    }

    public static void markSupported(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            SUPPORTED_PLAYERS.put(player, player.serverLevel().getGameTime());
        }
    }

    public static boolean isSupported(ServerPlayer player) {
        Long supportedAt = SUPPORTED_PLAYERS.get(player);
        if (supportedAt == null) {
            return false;
        }

        long age = player.serverLevel().getGameTime() - supportedAt;
        if (age >= 0L && age <= SUPPORT_GRACE_TICKS) {
            return true;
        }

        SUPPORTED_PLAYERS.remove(player);
        return false;
    }
}
