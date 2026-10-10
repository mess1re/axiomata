package me.mss1r.axiomata.blueprint.client;

import me.mss1r.axiomata.blueprint.tracing.BlueprintOutline;
import me.mss1r.axiomata.blueprint.tracing.OutlineCatalog;
import me.mss1r.axiomata.blueprint.tracing.OutlineMask;
import me.mss1r.axiomata.blueprint.tracing.TracingRules;
import me.mss1r.axiomata.blueprint.tracing.TracingSession;
import me.mss1r.axiomata.blueprint.network.C2SStrokePacket;
import me.mss1r.axiomata.blueprint.network.NetworkHandler;
import me.mss1r.axiomata.blueprint.network.S2CTracingStatePacket;

// Samples are applied locally in the same batches sent to the server. Drawing them immediately on
// mouse events looks a bit smoother, but makes the two sessions disagree under lag.
public final class ClientTracing {
    private static String blueprintId = "";
    private static BlueprintOutline outline;
    private static TracingSession session;

    private static final byte[] pending = new byte[TracingRules.MAX_SAMPLES_PER_TICK * 2];
    private static int pendingCount;
    private static boolean pendingLift;

    private ClientTracing() {
    }

    public static void accept(S2CTracingStatePacket packet) {
        try {
            OutlineMask mask = new OutlineMask(packet.resolution(), packet.mask());
            outline = new BlueprintOutline(packet.texture(), mask);
            blueprintId = packet.blueprintId();
            OutlineCatalog.remember(blueprintId, outline);
            session = outline.openSession(TracingRules.TOLERANCE_PIXELS);
            if (packet.covered().length > 0) {
                session.restore(packet.covered(), packet.wandered());
            }
        } catch (IllegalArgumentException exception) {
            clear();
        }
        pendingCount = 0;
        pendingLift = false;
    }

    public static void clear() {
        blueprintId = "";
        outline = null;
        session = null;
        pendingCount = 0;
        pendingLift = false;
    }

    public static String blueprintId() {
        return blueprintId;
    }

    public static BlueprintOutline outline() {
        return outline;
    }

    public static TracingSession session() {
        return session;
    }
    public static void sample(int x, int y) {
        if (session == null) {
            return;
        }
        int clampedX = Math.max(0, Math.min(TracingRules.CANVAS_PIXELS - 1, x));
        int clampedY = Math.max(0, Math.min(TracingRules.CANVAS_PIXELS - 1, y));
        // Once the tick's buffer is full, retain the endpoint. Interpolation covers the skipped
        // distance; losing the endpoint would leave the ink trailing behind the cursor.
        int slot = pendingCount < TracingRules.MAX_SAMPLES_PER_TICK ? pendingCount++
                : TracingRules.MAX_SAMPLES_PER_TICK - 1;
        pending[slot * 2] = (byte) clampedX;
        pending[slot * 2 + 1] = (byte) clampedY;
    }

    public static void lift() {
        pendingLift = true;
    }

    public static void cancelStroke() {
        pendingCount = 0;
        pendingLift = true;
    }

    public static void flush() {
        if (session == null || (pendingCount == 0 && !pendingLift)) {
            return;
        }
        byte[] points = new byte[pendingCount * 2];
        System.arraycopy(pending, 0, points, 0, points.length);

        for (int index = 0; index < pendingCount; index++) {
            session.apply(points[index * 2] & 0xFF, points[index * 2 + 1] & 0xFF);
        }
        if (pendingLift) {
            session.lift();
        }

        NetworkHandler.sendToServer(new C2SStrokePacket(pendingLift, points));
        pendingCount = 0;
        pendingLift = false;
    }
}
