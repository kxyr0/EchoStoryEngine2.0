package io.github.kxyr0.echodialoguecutscene.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kxyr0.echocutscene.forge.api.service.CutsceneService;
import io.github.kxyr0.echodialoguecutscene.forge.wait.DialogueCutsceneServices;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Emergency escape command for authoring sessions stuck behind a screen fade.
 */
public final class EchoEscapeCommands {
    private final DialogueCutsceneServices services;

    public EchoEscapeCommands(DialogueCutsceneServices services) {
        this.services = Objects.requireNonNull(services, "services");
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_escape").requires(source -> source.hasPermission(2))
                .executes(context -> escape(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player()).executes(context -> escape(
                        context.getSource(), EntityArgument.getPlayer(context, "player"))))
                .then(Commands.literal("all").executes(context -> escapeAll(context.getSource()))));
    }

    private int escape(CommandSourceStack source, ServerPlayer player) {
        EscapeResult result = escapeOne(source, player);
        source.sendSuccess(() -> Component.literal("echo_escape player=" + player.getScoreboardName()
                + " black=" + result.black()
                + " cutsceneStop=" + result.cutsceneStop()
                + " dialogueCancel=" + result.dialogueCancel()), true);
        return result.success() ? 1 : 0;
    }

    private int escapeAll(CommandSourceStack source) throws CommandSyntaxException {
        List<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
        if (players.isEmpty()) {
            source.sendFailure(Component.literal("echo_escape: online players not found."));
            return 0;
        }
        int success = 0;
        for (ServerPlayer player : players) {
            EscapeResult result = escapeOne(source, player);
            if (result.success()) {
                success++;
            }
        }
        int finalSuccess = success;
        source.sendSuccess(() -> Component.literal("echo_escape all players=" + players.size()
                + " success=" + finalSuccess), true);
        return success;
    }

    private EscapeResult escapeOne(CommandSourceStack source, ServerPlayer player) {
        boolean black = clearBlack(source.getServer(), player);
        CommandSourceStack playerSource = source.withEntity(player).withPosition(player.position());
        int cutsceneStop = run(playerSource, "echo_cutscene stop");
        int dialogueCancel = run(playerSource, "echo_dialogue cancel");
        return new EscapeResult(black, cutsceneStop, dialogueCancel);
    }

    private boolean clearBlack(MinecraftServer server, ServerPlayer player) {
        Optional<CutsceneService> cutscene = services.cutscene(server);
        cutscene.ifPresent(service -> service.setBlack(player, 0.0D, 0L));
        return cutscene.isPresent();
    }

    private int run(CommandSourceStack source, String command) {
        try {
            return source.getServer().getCommands().performPrefixedCommand(source, command);
        } catch (RuntimeException exception) {
            source.sendFailure(Component.literal("echo_escape failed to run /" + command
                    + ": " + exception.getMessage()));
            return 0;
        }
    }

    private record EscapeResult(boolean black, int cutsceneStop, int dialogueCancel) {
        boolean success() {
            return black || cutsceneStop > 0 || dialogueCancel > 0;
        }
    }
}
