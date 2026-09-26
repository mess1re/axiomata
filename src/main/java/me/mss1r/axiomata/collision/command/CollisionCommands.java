package me.mss1r.axiomata.collision.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.mss1r.axiomata.collision.network.CollisionNetworkHandler;
import me.mss1r.axiomata.collision.network.SetCollisionDebugS2CPacket;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class CollisionCommands {
    private CollisionCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                CommandBuildContext registry,
                                Commands.CommandSelection selection) {
        dispatcher.register(Commands.literal("axiomata")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("debug")
                        .then(Commands.literal("collision")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> setCollisionRendering(
                                                context,
                                                BoolArgumentType.getBool(context, "enabled")))))));
    }

    private static int setCollisionRendering(CommandContext<CommandSourceStack> context, boolean enabled)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CollisionNetworkHandler.sendToPlayer(player, new SetCollisionDebugS2CPacket(enabled));
        context.getSource().sendSuccess(() -> Component.literal(
                "Axiomata collision debug: " + (enabled ? "on" : "off")), false);
        return 1;
    }
}
