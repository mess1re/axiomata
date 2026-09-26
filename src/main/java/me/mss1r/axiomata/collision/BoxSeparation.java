package me.mss1r.axiomata.collision;

import net.minecraft.world.phys.Vec3;

public final class BoxSeparation {
    public static final double NO_IMPACT = -1.0D;

    private static final double CLEARANCE = 1.0E-4D;
    private static final double TIME_EPSILON = 1.0E-8D;

    private final Vec3 stepAxis;
    private Vec3 shallowestAxis;
    private double shallowestDepth = Double.MAX_VALUE;
    private double stepDepth = Double.MAX_VALUE;

    private boolean overlapping;
    private boolean hasEntry;
    private double latestEntry = Double.NEGATIVE_INFINITY;
    private double earliestExit = Double.POSITIVE_INFINITY;
    private Vec3 impactNormal;

    BoxSeparation(Vec3 stepAxis) {
        this.stepAxis = stepAxis.normalize();
    }

    public boolean isOverlapping() {
        return overlapping;
    }

    public double timeOfImpact() {
        if (overlapping || !hasEntry || latestEntry > earliestExit
                || earliestExit <= TIME_EPSILON || latestEntry > 1.0D) {
            return NO_IMPACT;
        }
        return Math.max(0.0D, latestEntry);
    }

    public Vec3 contactNormal() {
        return overlapping ? shallowestAxis : impactNormal;
    }

    public Vec3 pushOut(double stepHeight) {
        if (!overlapping) {
            return timeOfImpact() == NO_IMPACT ? null : Vec3.ZERO;
        }
        if (stepDepth <= stepHeight) {
            return stepAxis.scale(stepDepth + CLEARANCE);
        }
        if (shallowestAxis == null) {
            return null;
        }
        return shallowestAxis.scale(shallowestDepth + CLEARANCE);
    }

    void markOverlapping() {
        overlapping = true;
    }

    void recordEntry(double entry, Vec3 normal) {
        if (!hasEntry || entry > latestEntry + TIME_EPSILON) {
            latestEntry = entry;
            impactNormal = normal;
        }
        hasEntry = true;
    }

    void recordExit(double exit) {
        earliestExit = Math.min(earliestExit, exit);
    }

    void recordShallowest(Vec3 axis, double depth) {
        if (depth < shallowestDepth) {
            shallowestAxis = axis;
            shallowestDepth = depth;
        }
    }

    void recordStep(double depth) {
        if (depth >= 0.0D && depth < stepDepth) {
            stepDepth = depth;
        }
    }

    Vec3 stepAxis() {
        return stepAxis;
    }
}
