package me.mss1r.axiomata.loading;

import net.minecraft.world.item.Item;

import java.util.Objects;

public record LoadingRequirement(Item item, int amount, ItemPolicy itemPolicy,
                                 String feedback, String timingKey) {
    private static final String DEFAULT_TIMING_KEY = "ammunition";

    public LoadingRequirement {
        item = Objects.requireNonNull(item, "item");
        itemPolicy = Objects.requireNonNull(itemPolicy, "itemPolicy");
        feedback = Objects.requireNonNull(feedback, "feedback");
        timingKey = Objects.requireNonNull(timingKey, "timingKey");
        if (amount < 1) {
            throw new IllegalArgumentException("Loading amount must be positive");
        }
        if (timingKey.isBlank()) {
            throw new IllegalArgumentException("Loading timing key must not be blank");
        }
    }

    public static LoadingRequirement consume(Item item) {
        return consume(item, 1);
    }

    public static LoadingRequirement consume(Item item, int amount) {
        return new LoadingRequirement(item, amount, ItemPolicy.CONSUME,
                "", DEFAULT_TIMING_KEY);
    }

    public static LoadingRequirement tool(Item item) {
        return tool(item, 1);
    }

    public static LoadingRequirement tool(Item item, int durabilityCost) {
        return new LoadingRequirement(item, durabilityCost, ItemPolicy.DAMAGE_TOOL,
                "", DEFAULT_TIMING_KEY);
    }

    public LoadingRequirement withFeedback(String replacement) {
        return new LoadingRequirement(item, amount, itemPolicy, replacement, timingKey);
    }

    public LoadingRequirement timedBy(String replacement) {
        return new LoadingRequirement(item, amount, itemPolicy, feedback, replacement);
    }

    public boolean matches(Item candidate) {
        return item == candidate;
    }

    public int requiredStackCount() {
        return itemPolicy == ItemPolicy.CONSUME ? amount : 1;
    }

    public int durabilityCost() {
        return itemPolicy == ItemPolicy.DAMAGE_TOOL ? amount : 0;
    }

    public enum ItemPolicy {
        CONSUME,
        DAMAGE_TOOL
    }
}
