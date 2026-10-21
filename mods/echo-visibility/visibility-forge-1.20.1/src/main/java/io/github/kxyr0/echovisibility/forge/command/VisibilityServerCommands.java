package io.github.kxyr0.echovisibility.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.forge.runtime.VisibilityServerHost;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class VisibilityServerCommands {
    private VisibilityServerCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            VisibilityServerHost host) {
        dispatcher.register(Commands.literal("echo_visibility")
                .then(Commands.literal("arm").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("watch", StringArgumentType.word())
                                .executes(context -> arm(context.getSource(), host,
                                        StringArgumentType.getString(context, "watch")))))
                .then(Commands.literal("disarm").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("watch", StringArgumentType.word())
                                .executes(context -> disarm(context.getSource(), host,
                                        StringArgumentType.getString(context, "watch")))))
                .then(Commands.literal("assert_out_of_view").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("watch", StringArgumentType.word())
                                .executes(context -> assertOut(context.getSource(), host,
                                        StringArgumentType.getString(context, "watch")))))
                .then(Commands.literal("reload").requires(source -> source.hasPermission(2))
                        .executes(context -> reload(context.getSource(), host)))
                .then(Commands.literal("status").executes(context -> status(context.getSource(), host))));
    }

    private static int arm(CommandSourceStack source, VisibilityServerHost host, String raw)
            throws CommandSyntaxException {
        var result = host.service().arm(source.getPlayerOrException(), id(raw), "command:" + raw);
        return result(source, "Arm", result.status().name());
    }

    private static int disarm(CommandSourceStack source, VisibilityServerHost host, String raw)
            throws CommandSyntaxException {
        var result = host.service().disarm(source.getPlayerOrException(), id(raw));
        return result(source, "Disarm", result.status().name());
    }

    private static int assertOut(CommandSourceStack source, VisibilityServerHost host, String raw)
            throws CommandSyntaxException {
        var result = host.service().assertStillOutOfView(source.getPlayerOrException(), id(raw));
        return result(source, "Blind window", result.status().name());
    }

    private static int reload(CommandSourceStack source, VisibilityServerHost host) {
        var result = host.reload();
        source.sendSuccess(() -> Component.literal("Visibility reload: loaded="
                + result.loadedDefinitions() + ", parser=" + result.parserCalls()
                + ", binder=" + result.binderCalls() + ", diagnostics="
                + result.diagnostics().size()), true);
        return result.diagnostics().isEmpty() ? 1 : 0;
    }

    private static int status(CommandSourceStack source, VisibilityServerHost host) {
        int active = source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player
                ? host.query().active(player).size() : 0;
        source.sendSuccess(() -> Component.literal("Visibility catalog="
                + host.query().all().size() + ", active=" + active), false);
        return 1;
    }

    private static int result(CommandSourceStack source, String operation, String status) {
        source.sendSuccess(() -> Component.literal(operation + ": " + status), false);
        return status.equals("ARMED") || status.equals("DISARMED") || status.equals("ASSERTED") ? 1 : 0;
    }

    private static VisibilityWatchId id(String raw) {
        return new VisibilityWatchId(raw);
    }
}
