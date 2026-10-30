package me.mss1r.axiomata;

import me.mss1r.axiomata.blueprint.BlueprintModule;
import me.mss1r.axiomata.ballistics.BallisticsModule;
import me.mss1r.axiomata.collision.CollisionModule;
import me.mss1r.axiomata.config.AxiomataClientConfig;
import me.mss1r.axiomata.client.AxiomataUpdateNotifier;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Axiomata.MOD_ID)
public final class AxiomataBootstrap {
    public AxiomataBootstrap(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, AxiomataClientConfig.SPEC);
        BlueprintModule.initialize(modEventBus, modContainer, net.neoforged.neoforge.common.NeoForge.EVENT_BUS);
        CollisionModule.initialize();
        BallisticsModule.initialize(modEventBus, net.neoforged.neoforge.common.NeoForge.EVENT_BUS);
        initializeUpdateNotices();
    }

    private static void initializeUpdateNotices() {
        EnvExecutor.runInEnv(Env.CLIENT, () -> AxiomataUpdateNotifier::register);
    }
}
