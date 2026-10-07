package me.mss1r.axiomata.ballistics.damage;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Sends block crack overlays to nearby players. */
final class BlockCrackOverlay {
    private static final double RANGE = 64.0D;

    private BlockCrackOverlay() {
    }

    static void show(ServerLevel level, BlockPos pos, float progress) {
        send(level, pos, Math.min(9, (int) (progress * 10.0F)));
    }

    static void clear(ServerLevel level, BlockPos pos) {
        send(level, pos, -1);
    }

    private static void send(ServerLevel level, BlockPos pos, int stage) {
        ClientboundBlockDestructionPacket packet =
                new ClientboundBlockDestructionPacket(Long.hashCode(pos.asLong()), pos, stage);
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().closerThan(pos, RANGE)) {
                player.connection.send(packet);
            }
        }
    }
}
