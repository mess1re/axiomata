package me.mss1r.axiomata;

import me.mss1r.axiomata.blueprint.BlueprintModule;
import me.mss1r.axiomata.collision.CollisionModule;
//? if forge {
/*import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
*///?} else {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
//?}

@Mod(Axiomata.MOD_ID)
public final class AxiomataBootstrap {
    //? if forge {
    /*public AxiomataBootstrap() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(Axiomata.MOD_ID, modEventBus);
        BlueprintModule.initialize(modEventBus, MinecraftForge.EVENT_BUS);
        CollisionModule.initialize();
    }
    *///?} else {
    public AxiomataBootstrap(IEventBus modEventBus, ModContainer modContainer) {
        BlueprintModule.initialize(modEventBus, modContainer, NeoForge.EVENT_BUS);
        CollisionModule.initialize();
    }
    //?}
}

