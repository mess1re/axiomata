package me.mss1r.axiomata.blueprint.config;

import me.mss1r.axiomata.Axiomata;

//? if forge {
/*import net.minecraftforge.common.ForgeConfigSpec;
*///?} else {
import net.neoforged.neoforge.common.ModConfigSpec;
//?}

public final class BlueprintServerConfig {
    //? if forge {
    /*public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.DoubleValue HITS_PER_ITEM;
    private static final ForgeConfigSpec.IntValue MIN_STAGE_HITS;
    private static final ForgeConfigSpec.IntValue MAX_STAGE_HITS;
    private static final ForgeConfigSpec.IntValue HIT_COOLDOWN_TICKS;
    *///?} else {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.DoubleValue HITS_PER_ITEM;
    private static final ModConfigSpec.IntValue MIN_STAGE_HITS;
    private static final ModConfigSpec.IntValue MAX_STAGE_HITS;
    private static final ModConfigSpec.IntValue HIT_COOLDOWN_TICKS;
    //?}

    static {
        //? if forge {
        /*ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        *///?} else {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        //?}
        builder.comment("Config layout version, not the mod version. Leave this unchanged.",
                        "Adding settings does not change the layout version.")
                .defineInRange("configVersion", Axiomata.CONFIG_FORMAT_VERSION, 1, Integer.MAX_VALUE);
        builder.push("construction");

        HITS_PER_ITEM = builder
                .comment("Hammer blows a construction stage takes for each item it fits.")
                .defineInRange("hitsPerItem", 1.0D, 0.05D, 20.0D);
        MIN_STAGE_HITS = builder
                .comment("Fewest blows any stage can take, however little it costs.")
                .defineInRange("minStageHits", 3, 1, 1000);
        MAX_STAGE_HITS = builder
                .comment("Hard safety ceiling for the number of blows in one construction stage."
                        + " Authored heavy-machine stages are expected to exceed twenty blows.")
                .defineInRange("maxStageHits", 100, 1, 1000);
        HIT_COOLDOWN_TICKS = builder
                .comment("Minimum delay in ticks between accepted hammer blows from one builder.")
                .defineInRange("hitCooldownTicks", 8, 0, 1200);

        builder.pop();
        SPEC = builder.build();
    }

    private BlueprintServerConfig() {
    }

    public static double getHitsPerItem() {
        return HITS_PER_ITEM.get();
    }

    public static int getMinStageHits() {
        return MIN_STAGE_HITS.get();
    }

    public static int getMaxStageHits() {
        return Math.max(getMinStageHits(), MAX_STAGE_HITS.get());
    }

    public static int getHitCooldownTicks() {
        return HIT_COOLDOWN_TICKS.get();
    }
}
