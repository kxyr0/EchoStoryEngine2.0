package io.github.kxyr0.echocore.features;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echocore.api.FeatureFlags;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class FeatureCommands {
    private FeatureCommands() { }
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("echo_core").then(Commands.literal("flag")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("players", EntityArgument.players())
                .then(Commands.argument("flag", StringArgumentType.word())
                .suggests((context, builder) -> { builder.suggest(FeatureFlags.THOUGHT_SPACE); builder.suggest(FeatureFlags.MAGIC); return builder.buildFuture(); })
                .executes(context -> {
                    String flag = StringArgumentType.getString(context, "flag");
                    if (!flag.matches("[a-z][a-z0-9_]{0,63}")) {
                        context.getSource().sendFailure(Component.literal("Invalid feature flag")); return 0;
                    }
                    for (ServerPlayer player : EntityArgument.getPlayers(context, "players")) {
                        boolean value = FeatureFlags.serverAllowed(player.getUUID(), flag);
                        context.getSource().sendSuccess(() -> Component.literal(player.getScoreboardName() + ": " + flag + "=" + value), false);
                    }
                    return 1;
                })
                .then(Commands.argument("value", BoolArgumentType.bool()).executes(context -> {
                    String flag = StringArgumentType.getString(context, "flag");
                    if (!flag.matches("[a-z][a-z0-9_]{0,63}")) {
                        context.getSource().sendFailure(Component.literal("Invalid feature flag")); return 0;
                    }
                    var players = EntityArgument.getPlayers(context, "players");
                    for (ServerPlayer player : players) FeatureFlags.set(player.getUUID(), flag, BoolArgumentType.getBool(context, "value"));
                    return players.size();
                }))))));
    }
}
