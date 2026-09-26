package me.mss1r.axiomata.blueprint.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.item.BlueprintItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.mss1r.axiomata.blueprint.api.construction.BuildQuality;
import net.minecraft.world.item.ItemStack;
import java.util.Arrays;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

public final class BlueprintCommands {
    private static final DynamicCommandExceptionType UNKNOWN_BLUEPRINT = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.axiomata.blueprint.unknown", id)
    );

    private BlueprintCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                net.minecraft.commands.CommandBuildContext registry,
                                Commands.CommandSelection selection) {
        var targetsArgument = Commands.argument("targets", EntityArgument.players())
                .executes(context -> giveBlueprint(
                        context,
                        EntityArgument.getPlayers(context, "targets"),
                        qualityOf(context)
                ));
        var qualityArgument = Commands.argument("quality", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        Arrays.stream(BuildQuality.values()).map(BuildQuality::id).toList(), builder))
                .executes(context -> giveBlueprint(
                        context,
                        List.of(context.getSource().getPlayerOrException()),
                        qualityOf(context)
                ))
                .then(targetsArgument);
        var blueprintArgument = Commands.argument("blueprint", ResourceLocationArgument.id())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        BlueprintDefinitions.allById().keySet(), builder))
                .executes(context -> giveBlueprint(
                        context,
                        List.of(context.getSource().getPlayerOrException()),
                        BuildQuality.PLAIN
                ))
                .then(qualityArgument);

        dispatcher.register(Commands.literal("axiomata")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("blueprint")
                        .then(Commands.literal("give")
                                .then(blueprintArgument))));
    }

    private static BuildQuality qualityOf(CommandContext<CommandSourceStack> context) {
        return BuildQuality.byId(StringArgumentType.getString(context, "quality"));
    }

    private static int giveBlueprint(CommandContext<CommandSourceStack> context,
                                     Collection<ServerPlayer> targets, BuildQuality quality)
            throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(context, "blueprint");
        BlueprintDefinition recipe = BlueprintDefinitions.get(id.toString());
        if (recipe == null) {
            throw UNKNOWN_BLUEPRINT.create(id);
        }

        for (ServerPlayer target : targets) {
            ItemStack blueprint = BlueprintItem.create(recipe, id.toString());
            BlueprintItem.stampQuality(blueprint, context.getSource().getTextName(), quality);
            if (!target.getInventory().add(blueprint)) {
                target.drop(blueprint, false);
            }
        }

        context.getSource().sendSuccess(
                () -> Component.translatable("commands.axiomata.blueprint.give.success", id, targets.size()),
                true
        );
        return targets.size();
    }
}
