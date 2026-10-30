package me.mss1r.axiomata.blueprint;
import net.neoforged.bus.api.IEventBus;
/** Loader-facing entry point retained for integrating mods. */
public final class BlueprintModule {
    public static final String MOD_ID = "axiomata";
    public static void initialize(IEventBus modBus, net.neoforged.fml.ModContainer container, IEventBus gameBus) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, me.mss1r.axiomata.blueprint.config.BlueprintServerConfig.SPEC);
        BlueprintCommon.initialize();
        me.mss1r.axiomata.neoforge.AxiomataEvents.registerBlueprint(modBus, gameBus);
    }
}
