package me.mss1r.axiomata.loading;

/** Shared action-bar progress for loading tools, weapons and machines. */
public final class LoadingProgress {
    private LoadingProgress() {}

    public static String bar(int doneTicks, int totalTicks) {
        int width = 10;
        int done = Math.max(0, doneTicks);
        int total = Math.max(1, totalTicks);
        int filled = Math.max(0, Math.min(width, Math.round(done * width / (float) total)));
        return "[" + "#".repeat(filled) + "-".repeat(width - filled) + "]";
    }
}
