package me.mss1r.axiomata.config;

import me.mss1r.axiomata.Axiomata;
//? if forge {
/*import net.minecraftforge.common.ForgeConfigSpec;
*///?} else {
import net.neoforged.neoforge.common.ModConfigSpec;
//?}

public final class AxiomataClientConfig {
    //? if forge {
    /*public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue SHOW_UPDATE_NOTICE;
    *///?} else {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue SHOW_UPDATE_NOTICE;
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
        builder.push("updates");
        SHOW_UPDATE_NOTICE = builder
                .comment("Show a download link in chat once per game launch when a newer version is available.",
                        "The loader checks for updates; this only controls the chat message.")
                .define("showNotice", true);
        builder.pop();
        SPEC = builder.build();
    }

    private AxiomataClientConfig() {
    }

    public static boolean showUpdateNotice() {
        return SHOW_UPDATE_NOTICE.get();
    }
}
