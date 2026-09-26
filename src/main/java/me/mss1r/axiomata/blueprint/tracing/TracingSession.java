package me.mss1r.axiomata.blueprint.tracing;

// The client uses this for feedback, but the server replays the same samples and decides the
// result. Cursor points snap to the nearest outline cell; accuracy is measured by how far the
// cursor wandered, not by the player's ability to draw a smooth line.
public final class TracingSession {
    private final OutlineMask mask;
    private final OutlineField field;
    private final int scale;
    private final int toleranceCells;
    private final long[] covered;

    private final int wanderBudget;

    private final int brushCells;

    private int coveredCells;
    private int wandered;
    private int lastX = Integer.MIN_VALUE;
    private int lastY = Integer.MIN_VALUE;

    public TracingSession(OutlineMask mask, OutlineField field, int tolerancePixels) {
        this.mask = mask;
        this.field = field;
        this.scale = TracingRules.CANVAS_PIXELS / mask.resolution();
        this.toleranceCells = Math.max(1, tolerancePixels / scale);
        this.covered = new long[(mask.resolution() * mask.resolution() + 63) / 64];
        this.wanderBudget = TracingRules.wanderBudget(mask.cells());
        this.brushCells = Math.max(1, TracingRules.COVER_RADIUS_PIXELS / scale);
    }
    public boolean apply(int x, int y) {
        boolean drawn = false;
        if (lastX == Integer.MIN_VALUE) {
            drawn = sample(x, y);
        } else {
            int steps = Math.max(Math.abs(x - lastX), Math.abs(y - lastY));
            // Fill gaps from low frame rate and from batched packets. Capping at one canvas width
            // also keeps a bogus coordinate from turning into an unbounded loop.
            steps = Math.min(steps, TracingRules.CANVAS_PIXELS);
            for (int step = 1; step <= steps; step++) {
                int pointX = lastX + (x - lastX) * step / steps;
                int pointY = lastY + (y - lastY) * step / steps;
                drawn |= sample(pointX, pointY);
            }
            if (steps == 0) {
                drawn = sample(x, y);
            }
        }
        lastX = x;
        lastY = y;
        return drawn;
    }

    private boolean sample(int x, int y) {
        int cellX = x / scale;
        int cellY = y / scale;
        if (!mask.contains(cellX, cellY)) {
            wandered++;
            return false;
        }
        int distance = field.distanceSquared(cellX, cellY);
        if (distance > toleranceCells * toleranceCells) {
            wandered++;
            return false;
        }
        paint(field.nearestX(cellX, cellY), field.nearestY(cellX, cellY));
        return true;
    }

    private void paint(int centreX, int centreY) {
        for (int y = centreY - brushCells; y <= centreY + brushCells; y++) {
            for (int x = centreX - brushCells; x <= centreX + brushCells; x++) {
                int dx = x - centreX;
                int dy = y - centreY;
                if (dx * dx + dy * dy > brushCells * brushCells || !mask.ink(x, y)) {
                    continue;
                }
                int index = y * mask.resolution() + x;
                long bit = 1L << (index & 63);
                if ((covered[index >> 6] & bit) == 0) {
                    covered[index >> 6] |= bit;
                    coveredCells++;
                }
            }
        }
    }

    public void lift() {
        lastX = Integer.MIN_VALUE;
        lastY = Integer.MIN_VALUE;
    }

    public boolean covered(int x, int y) {
        int index = y * mask.resolution() + x;
        return (covered[index >> 6] & (1L << (index & 63))) != 0;
    }

    public float coverage() {
        return mask.cells() == 0 ? 0.0F : (float) coveredCells / mask.cells();
    }

    public int wandered() {
        return wandered;
    }

    public int wanderBudget() {
        return wanderBudget;
    }

    public boolean complete() {
        return coverage() >= TracingRules.COVERAGE_TARGET;
    }

    public boolean ruined() {
        return wandered >= wanderBudget;
    }

    public OutlineMask mask() {
        return mask;
    }

    public byte[] snapshot() {
        byte[] out = new byte[covered.length * 8];
        for (int index = 0; index < covered.length; index++) {
            long word = covered[index];
            for (int shift = 0; shift < 8; shift++) {
                out[index * 8 + shift] = (byte) (word >>> (shift * 8));
            }
        }
        return out;
    }

    public void restore(byte[] snapshot, int wandered) {
        if (snapshot.length != covered.length * 8) {
            return;
        }
        coveredCells = 0;
        for (int index = 0; index < covered.length; index++) {
            long word = 0L;
            for (int shift = 0; shift < 8; shift++) {
                word |= (snapshot[index * 8 + shift] & 0xFFL) << (shift * 8);
            }
            covered[index] = word;
            coveredCells += Long.bitCount(word);
        }
        this.wandered = wandered;
        lift();
    }
}
