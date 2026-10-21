package io.github.kxyr0.echostoryengine.blackspace.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kxyr0.echostoryengine.blackspace.api.BlackspaceInstance;
import io.github.kxyr0.echostoryengine.blackspace.api.BlackspaceScene;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class BlackspaceDebugCommand {
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo").requires(source -> source.hasPermission(2))
                .then(Commands.literal("blackspace")
                        .then(Commands.literal("start")
                                .executes(context -> start(context.getSource(),
                                        context.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> start(context.getSource(),
                                                EntityArgument.getPlayer(context, "player")))))
                        .then(Commands.literal("stop")
                                .executes(context -> stop(context.getSource(),
                                        context.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> stop(context.getSource(),
                                                EntityArgument.getPlayer(context, "player")))))));
    }

    private int start(CommandSourceStack source, ServerPlayer player) throws CommandSyntaxException {
        BlackspaceInstance instance = BlackspaceScene.create(player, "echo_blackspace:debug");
        BlackspaceScene.teleportTo(player, instance);
        source.sendSuccess(() -> Component.literal("Blackspace ready for "
                + player.getGameProfile().getName() + " at " + instance.spawnValue()), false);
        return 1;
    }

    private int stop(CommandSourceStack source, ServerPlayer player) throws CommandSyntaxException {
        BlackspaceScene.stop(player);
        source.sendSuccess(() -> Component.literal("Blackspace disabled for "
                + player.getGameProfile().getName()), false);
        return 1;
    }
}
