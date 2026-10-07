package me.mss1r.axiomata.ballistics;


import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Keeps simulating shots past the simulation distance, where the server stops ticking entities, as long as their chunks
 * are loaded; otherwise a shot at a distant wall would hang in the air. Shots that leave loaded chunks are removed.
 */
public final class DistantFlight {
    private static final Set<BallisticProjectile> SHOTS = Collections.newSetFromMap(new WeakHashMap<>());

    private DistantFlight() {
    }

    public static void track(BallisticProjectile shot) {
        SHOTS.add(shot);
    }

    public static void tick(MinecraftServer server) {
        if (SHOTS.isEmpty()) {
            return;
        }
        for (BallisticProjectile shot : List.copyOf(SHOTS)) {
            if (!shot.isInFlight() || !(shot.level() instanceof ServerLevel level) || level.getServer() != server) {
                SHOTS.remove(shot);
                continue;
            }
            long now = level.getGameTime();
            if (shot.flewOn(now)) {
                continue;
            }
            if (!shot.flewOn(now - 1L)) {
                SHOTS.remove(shot);
                continue;
            }
            if (level.hasChunkAt(shot.blockPosition())) {
                level.guardEntityTick(level::tickNonPassenger, shot);
            }
            if (!shot.isRemoved() && !level.hasChunkAt(shot.blockPosition())) {
                shot.discard();
            }
        }
    }
}
