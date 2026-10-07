package me.mss1r.axiomata.data.profile;

import java.util.Map;
import java.util.Optional;

public final class ProfileValidation {
    private ProfileValidation() {
    }

    public static Optional<String> nonNegative(String name, double value) {
        if (!Double.isFinite(value)) {
            return Optional.of(name + " must be finite");
        }
        if (value < 0.0D) {
            return Optional.of(name + " must not be negative");
        }
        return Optional.empty();
    }

    public static Optional<String> nonNegative(String name, float value) {
        return nonNegative(name, (double) value);
    }

    public static Optional<String> nonNegativeValues(String name, Map<String, Double> values) {
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            Optional<String> error = nonNegative(name + "['" + entry.getKey() + "']", entry.getValue());
            if (error.isPresent()) {
                return error;
            }
        }
        return Optional.empty();
    }
}
