package me.mss1r.axiomata.blueprint.tracing;

public final class TracingRules {
    public static final int CANVAS_PIXELS = 256;

    public static final int TOLERANCE_PIXELS = 3;

    // Keep the server slightly more permissive than the preview. Rounding from pixels to mask
    // cells can otherwise reject a stroke that visibly snapped to the line on the client.
    public static final int SERVER_TOLERANCE_PIXELS = TOLERANCE_PIXELS + 2;

    public static final int MAX_SAMPLES_PER_TICK = 8;

    // Snapping already keeps the line clean. A wider brush would mostly make fast scribbling pay.
    public static final int COVER_RADIUS_PIXELS = 2;

    // Leave a little room for isolated missed cells without accepting visibly unfinished sheets.
    public static final float COVERAGE_TARGET = 0.9875F;

    // This counts off-line distance, not bad ticks. Crossing the outline while scribbling should
    // not reset the penalty, and larger drawings need proportionally more room for mistakes.
    public static final int WANDER_BASE_PIXELS = 250;
    public static final int WANDER_PIXELS_PER_CELL = 8;

    public static int wanderBudget(int cells) {
        return WANDER_BASE_PIXELS + cells / WANDER_PIXELS_PER_CELL;
    }

    private TracingRules() {
    }
}
