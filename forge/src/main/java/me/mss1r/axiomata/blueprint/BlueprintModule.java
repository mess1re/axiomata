package me.mss1r.axiomata.blueprint;
import net.minecraftforge.eventbus.api.IEventBus;
/** Loader-facing entry point retained for integrating mods. */
public final class BlueprintModule {
    public static final String MOD_ID = "axiomata";
    public static void initialize(IEventBus modBus, IEventBus gameBus) {
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER, me.mss1r.axiomata.blueprint.config.BlueprintServerConfig.SPEC);
        BlueprintCommon.initialize();
        me.mss1r.axiomata.forge.AxiomataEvents.registerBlueprint(modBus, gameBus);
    }
}
