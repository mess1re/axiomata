package me.mss1r.axiomata;

import me.mss1r.axiomata.blueprint.BlueprintModule;
import me.mss1r.axiomata.ballistics.BallisticsModule;
import me.mss1r.axiomata.collision.CollisionModule;
import me.mss1r.axiomata.config.AxiomataClientConfig;
import me.mss1r.axiomata.client.AxiomataUpdateNotifier;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
//? if forge {
/*import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
*///?} else {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
//?}

@Mod(Axiomata.MOD_ID)
public final class AxiomataBootstrap {
    //? if forge {
    /*public AxiomataBootstrap() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(Axiomata.MOD_ID, modEventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, AxiomataClientConfig.SPEC);
        BlueprintModule.initialize(modEventBus, MinecraftForge.EVENT_BUS);
        CollisionModule.initialize();
        BallisticsModule.initialize(modEventBus, MinecraftForge.EVENT_BUS);
        initializeUpdateNotices();
    }
    *///?} else {
    public AxiomataBootstrap(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, AxiomataClientConfig.SPEC);
        BlueprintModule.initialize(modEventBus, modContainer, NeoForge.EVENT_BUS);
        CollisionModule.initialize();
        BallisticsModule.initialize(modEventBus, NeoForge.EVENT_BUS);
        initializeUpdateNotices();
    }
    //?}

    private static void initializeUpdateNotices() {
        EnvExecutor.runInEnv(Env.CLIENT, () -> AxiomataUpdateNotifier::register);
    }
}

