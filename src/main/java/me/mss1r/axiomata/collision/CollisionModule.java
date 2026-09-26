package me.mss1r.axiomata.collision;

import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import me.mss1r.axiomata.collision.client.StructureCollisionDebugRenderer;
import me.mss1r.axiomata.collision.command.CollisionCommands;
import me.mss1r.axiomata.collision.network.CollisionNetworkHandler;

public final class CollisionModule {
    private static boolean initialized;

    private CollisionModule() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        CollisionNetworkHandler.register();
        CommandRegistrationEvent.EVENT.register(CollisionCommands::register);
        EnvExecutor.runInEnv(Env.CLIENT, () -> CollisionModule::initializeClient);
    }

    private static void initializeClient() {
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player ->
                StructureCollisionDebugRenderer.setEnabled(false));
    }
}
