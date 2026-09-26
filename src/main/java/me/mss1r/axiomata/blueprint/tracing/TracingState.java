package me.mss1r.axiomata.blueprint.tracing;

public enum TracingState {
    IDLE,
    READY,
    DRAWING,
    DONE,
    RUINED;

    private static final TracingState[] VALUES = values();

    public static TracingState byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : IDLE;
    }
}
