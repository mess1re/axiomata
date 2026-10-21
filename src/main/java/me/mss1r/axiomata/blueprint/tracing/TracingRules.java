package me.mss1r.axiomata.blueprint.tracing;

public final class TracingRules {
    public static final int CANVAS_PIXELS = 256;

    public static final int TOLERANCE_PIXELS = 3;

    // Keep the server slightly more permissive than the preview. Rounding from pixels to mask
    // cells can otherwise reject a stroke that visibly snapped to the line on the client.
    public static final int SERVER_TOLERANCE_PIXELS = TOLERANCE_PIXELS + 2;

    public static final int MAX_SAMPLES_PER_TICK = 8;

    // Wide enough to take both strokes of a doubled beam in one pass. Snapping still keeps the ink on
    // the line, and wandering off it is counted as before.
    public static final float COVER_RADIUS_PIXELS = 3.0F;

    // A few stray cells of a large drawing may stay dry; a missed stretch of line may not.
    public static final float COVERAGE_TARGET = 0.9975F;

    // A gap this small, left between inked parts of a line, is filled when the pen is lifted.
    public static final int MAX_GAP_CELLS = 4;

    // From this coverage on, the parts still to ink are pointed out on the sheet.
    public static final float HINT_COVERAGE = 0.9F;

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
