package me.mss1r.axiomata.collision;

import java.util.Arrays;

public final class ScalarAnimationCurve {
    public enum Interpolation {
        LINEAR,
        CATMULL_ROM,
        EASE_IN_ELASTIC,
        EASE_OUT_ELASTIC
    }

    public record Keyframe(float tick, double value, Interpolation interpolation) {
        public Keyframe {
            if (!Float.isFinite(tick) || !Double.isFinite(value)) {
                throw new IllegalArgumentException("Animation keyframes must be finite");
            }
        }
    }

    private final Keyframe[] keyframes;

    private ScalarAnimationCurve(Keyframe[] keyframes) {
        this.keyframes = keyframes;
    }

    public static ScalarAnimationCurve of(Keyframe... keyframes) {
        if (keyframes.length < 2) {
            throw new IllegalArgumentException("An animation curve needs at least two keyframes");
        }
        Keyframe[] copy = Arrays.copyOf(keyframes, keyframes.length);
        for (int i = 1; i < copy.length; i++) {
            if (copy[i].tick() <= copy[i - 1].tick()) {
                throw new IllegalArgumentException("Animation keyframe ticks must be strictly increasing");
            }
        }
        return new ScalarAnimationCurve(copy);
    }

    public static Keyframe key(float tick, double value, Interpolation interpolation) {
        return new Keyframe(tick, value, interpolation);
    }

    public double sample(float tick) {
        if (tick <= keyframes[0].tick()) {
            return keyframes[0].value();
        }
        int last = keyframes.length - 1;
        if (tick >= keyframes[last].tick()) {
            return keyframes[last].value();
        }

        int endIndex = 1;
        while (tick > keyframes[endIndex].tick()) {
            endIndex++;
        }
        Keyframe start = keyframes[endIndex - 1];
        Keyframe end = keyframes[endIndex];
        double progress = (tick - start.tick()) / (end.tick() - start.tick());

        if (end.interpolation() == Interpolation.CATMULL_ROM) {
            double previous = keyframes[Math.max(0, endIndex - 2)].value();
            double next = keyframes[Math.min(last, endIndex + 1)].value();
            return catmullRom(progress, previous, start.value(), end.value(), next);
        }

        double eased = switch (end.interpolation()) {
            case LINEAR -> progress;
            case EASE_IN_ELASTIC -> elastic(progress);
            case EASE_OUT_ELASTIC -> 1.0D - elastic(1.0D - progress);
            case CATMULL_ROM -> throw new IllegalStateException("Handled above");
        };
        return start.value() + (end.value() - start.value()) * eased;
    }

    private static double elastic(double progress) {
        return 1.0D - Math.pow(Math.cos(progress * Math.PI / 2.0D), 3.0D)
                * Math.cos(progress * Math.PI);
    }

    private static double catmullRom(double progress, double p0, double p1, double p2, double p3) {
        double progressSquared = progress * progress;
        return 0.5D * (2.0D * p1
                + (p2 - p0) * progress
                + (2.0D * p0 - 5.0D * p1 + 4.0D * p2 - p3) * progressSquared
                + (3.0D * p1 - p0 - 3.0D * p2 + p3) * progressSquared * progress);
    }
}
