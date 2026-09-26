package me.mss1r.axiomata.blueprint.api.construction;

import me.mss1r.axiomata.blueprint.config.BlueprintServerConfig;
import net.minecraft.ChatFormatting;

public enum BuildQuality {
    CLEAN("clean", 0.10F, 1.00F, 0.85F, ChatFormatting.AQUA),
    PLAIN("plain", 0.40F, 1.15F, 1.00F, ChatFormatting.GRAY),
    HASTY("hasty", 0.70F, 1.35F, 1.45F, ChatFormatting.YELLOW),
    SLOPPY("sloppy", Float.MAX_VALUE, 1.60F, 1.80F, ChatFormatting.RED);

    private static final BuildQuality[] VALUES = values();

    private final String id;
    private final float wanderLimit;
    private final float materialMultiplier;
    private final float hitMultiplier;
    private final ChatFormatting colour;

    BuildQuality(String id, float wanderLimit, float materialMultiplier, float hitMultiplier,
                 ChatFormatting colour) {
        this.id = id;
        this.wanderLimit = wanderLimit;
        this.materialMultiplier = materialMultiplier;
        this.hitMultiplier = hitMultiplier;
        this.colour = colour;
    }

    public ChatFormatting colour() {
        return colour;
    }

    public String id() {
        return id;
    }

    public static BuildQuality of(int wandered, int budget) {
        if (budget <= 0) {
            return PLAIN;
        }
        float share = wandered / (float) budget;
        for (BuildQuality quality : VALUES) {
            if (share <= quality.wanderLimit) {
                return quality;
            }
        }
        return SLOPPY;
    }

    public static BuildQuality byId(String id) {
        for (BuildQuality quality : VALUES) {
            if (quality.id.equals(id)) {
                return quality;
            }
        }
        return PLAIN;
    }

    public int materialsFor(int base) {
        if (base <= 0) {
            return 0;
        }
        // A clean drawing may save work, but it cannot make the machine require fewer parts than
        // its recipe. Material multipliers model waste only.
        return Math.max(base, (int) Math.ceil(base * materialMultiplier));
    }

    public int hitsFor(int base) {
        int adjusted = Math.round(base * hitMultiplier);
        return Math.max(BlueprintServerConfig.getMinStageHits(),
                Math.min(BlueprintServerConfig.getMaxStageHits(), adjusted));
    }
}
