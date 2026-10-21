package io.github.kxyr0.echodebugvisibilityadapter.forge.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echodebugvisibilityadapter.forge.client.VisibilityDebugAccess;
import io.github.kxyr0.echodebugvisibilityadapter.forge.client.VisibilityDebugSettings;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

public final class VisibilityDebugCommands {
    private VisibilityDebugCommands() {
    }

    public static void register(
            RegisterClientCommandsEvent event,
            VisibilityDebugSettings settings,
            VisibilityDebugAccess access) {
        event.getDispatcher().register(Commands.literal("echo_visibility")
                .then(Commands.literal("debug")
                        .then(Commands.literal("player")
                                .then(Commands.literal("on").executes(context ->
                                        player(context.getSource(), settings, true)))
                                .then(Commands.literal("off").executes(context ->
                                        player(context.getSource(), settings, false))))
                        .then(Commands.literal("watch")
                                .then(Commands.literal("off").executes(context ->
                                        watchOff(context.getSource(), settings)))
                                .then(Commands.argument("watch_id", StringArgumentType.word())
                                        .executes(context -> watch(context.getSource(), settings,
                                                StringArgumentType.getString(context, "watch_id")))))
                        .then(Commands.literal("all")
                                .then(Commands.literal("on").executes(context ->
                                        all(context.getSource(), settings, true)))
                                .then(Commands.literal("off").executes(context ->
                                        all(context.getSource(), settings, false))))
                        .then(Commands.literal("status").executes(context ->
                                status(context.getSource(), settings, access)))));
    }

    private static int player(CommandSourceStack source, VisibilityDebugSettings settings, boolean enabled) {
        settings.player(enabled);
        message(source, "Visibility debug player=" + enabled);
        return Command.SINGLE_SUCCESS;
    }

    private static int watch(CommandSourceStack source, VisibilityDebugSettings settings, String raw) {
        settings.watch(java.util.Optional.of(new VisibilityWatchId(raw)));
        message(source, "Visibility debug watch=" + raw);
        return Command.SINGLE_SUCCESS;
    }

    private static int watchOff(CommandSourceStack source, VisibilityDebugSettings settings) {
        settings.watch(java.util.Optional.empty());
        message(source, "Visibility debug watch=off");
        return Command.SINGLE_SUCCESS;
    }

    private static int all(CommandSourceStack source, VisibilityDebugSettings settings, boolean enabled) {
        settings.all(enabled);
        message(source, "Visibility debug all=" + enabled);
        return Command.SINGLE_SUCCESS;
    }

    private static int status(
            CommandSourceStack source,
            VisibilityDebugSettings settings,
            VisibilityDebugAccess access) {
        String provider = access.view().map(view -> "available watches=" + view.activeWatches().size()
                + " reports=" + view.performance().reportsSent()
                + " rays=" + view.performance().occlusionRays()).orElse("unavailable");
        message(source, "Visibility debug " + settings.status() + ", provider=" + provider);
        return Command.SINGLE_SUCCESS;
    }

    private static void message(CommandSourceStack source, String value) {
        source.sendSuccess(() -> Component.literal(value), false);
    }
}
