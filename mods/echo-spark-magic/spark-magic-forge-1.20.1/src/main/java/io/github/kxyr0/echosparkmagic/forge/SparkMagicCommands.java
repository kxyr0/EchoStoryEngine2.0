package io.github.kxyr0.echosparkmagic.forge;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SparkMagicCommands {
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_spark_magic")
                .requires(source -> source.hasPermission(2))
                .then(playerCommand("setup", true))
                .then(playerCommand("enable", true))
                .then(playerCommand("disable", false))
                .then(Commands.literal("status")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> status(context.getSource(),
                                        EntityArgument.getPlayer(context, "player"))))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> playerCommand(
            String name, boolean enabled) {
        return Commands.literal(name)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> set(context.getSource(),
                                EntityArgument.getPlayer(context, "player"), enabled)));
    }

    private int set(CommandSourceStack source, ServerPlayer player, boolean enabled) {
        SparkMagicPlayerState.setEnabled(player, enabled);
        return message(source, Component.translatable(enabled
                ? "command.echo_spark_magic.enabled" : "command.echo_spark_magic.disabled",
                player.getDisplayName()));
    }

    private int status(CommandSourceStack source, ServerPlayer player) {
        boolean enabled = SparkMagicPlayerState.enabled(player);
        return message(source, Component.translatable(enabled
                ? "command.echo_spark_magic.status.enabled"
                : "command.echo_spark_magic.status.disabled", player.getDisplayName()));
    }

    private int message(CommandSourceStack source, Component value) {
        source.sendSuccess(() -> value, false);
        return 1;
    }
}

