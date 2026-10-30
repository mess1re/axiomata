package me.mss1r.axiomata.fabric;

import me.mss1r.axiomata.Axiomata;
import me.mss1r.axiomata.blueprint.BlueprintCommon;
import me.mss1r.axiomata.ballistics.BallisticsCommon;
import me.mss1r.axiomata.collision.CollisionModule;
import me.mss1r.axiomata.config.AxiomataClientConfig;
import me.mss1r.axiomata.blueprint.config.BlueprintServerConfig;
import net.fabricmc.api.ModInitializer;
//? if >=1.21 {
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import net.neoforged.fml.config.ModConfig;
//?} else {
/*import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.minecraftforge.fml.config.ModConfig;
*///?}

public final class AxiomataFabric implements ModInitializer {
    @Override public void onInitialize() {
        //? if >=1.21 {
        NeoForgeConfigRegistry.INSTANCE.register(Axiomata.MOD_ID, ModConfig.Type.CLIENT, AxiomataClientConfig.SPEC);
        NeoForgeConfigRegistry.INSTANCE.register(Axiomata.MOD_ID, ModConfig.Type.SERVER, BlueprintServerConfig.SPEC);
        //?} else {
        /*ForgeConfigRegistry.INSTANCE.register(Axiomata.MOD_ID, ModConfig.Type.CLIENT, AxiomataClientConfig.SPEC);
        ForgeConfigRegistry.INSTANCE.register(Axiomata.MOD_ID, ModConfig.Type.SERVER, BlueprintServerConfig.SPEC);
        *///?}
        FabricEvents.register();
        BlueprintCommon.initialize();
        CollisionModule.initialize();
        BallisticsCommon.initialize();
    }
}
