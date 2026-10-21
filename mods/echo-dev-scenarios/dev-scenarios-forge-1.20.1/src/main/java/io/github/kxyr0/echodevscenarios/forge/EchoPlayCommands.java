package io.github.kxyr0.echodevscenarios.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echodevscenarios.api.ScenarioInspection;
import io.github.kxyr0.echodevscenarios.forge.network.ScenarioNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

final class EchoPlayCommands {
    private EchoPlayCommands() { }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher, ForgeScenarioHost host) {
        dispatcher.register(Commands.literal("echo_play").requires(source -> source.hasPermission(2))
                .executes(context -> open(context.getSource(), host))
                .then(Commands.literal("list").executes(context -> list(context.getSource(), host)))
                .then(Commands.literal("restart").executes(context -> restart(context.getSource(), host)))
                .then(Commands.literal("stop").executes(context -> stop(context.getSource(), host)))
                .then(Commands.literal("status").executes(context -> status(context.getSource(), host)))
                .then(Commands.argument("scenario", StringArgumentType.word()).executes(context -> play(
                        context.getSource(), host, StringArgumentType.getString(context, "scenario")))));
    }

    private static int open(CommandSourceStack source, ForgeScenarioHost host) {
        try {
            ScenarioNetwork.open(source.getPlayerOrException(), host.service().list());
            return 1;
        } catch (Exception exception) {
            return failure(source, exception.getMessage());
        }
    }

    private static int list(CommandSourceStack source, ForgeScenarioHost host) {
        source.sendSuccess(() -> Component.literal("Echo scenarios: " + host.service().list().size()), false);
        host.service().list().forEach(value -> source.sendSuccess(() -> Component.literal(value.id().value()
                + " | " + value.title() + " | " + value.target().resourceId()), false));
        return host.service().list().size();
    }

    private static int play(CommandSourceStack source, ForgeScenarioHost host, String id) {
        try {
            return success(source, host.play(source.getPlayerOrException(), id));
        } catch (Exception exception) {
            return failure(source, exception.getMessage());
        }
    }

    private static int restart(CommandSourceStack source, ForgeScenarioHost host) {
        try {
            return success(source, host.restart(source.getPlayerOrException()));
        } catch (Exception exception) {
            return failure(source, exception.getMessage());
        }
    }

    private static int stop(CommandSourceStack source, ForgeScenarioHost host) {
        try {
            return success(source, host.stop(source.getPlayerOrException()));
        } catch (Exception exception) {
            return failure(source, exception.getMessage());
        }
    }

    private static int status(CommandSourceStack source, ForgeScenarioHost host) {
        return host.service().status().map(value -> success(source, value))
                .orElseGet(() -> failure(source, "No active DevScenarioSession."));
    }

    private static int success(CommandSourceStack source, ScenarioInspection value) {
        source.sendSuccess(() -> Component.literal("Scenario=" + value.scenario().value() + " Session="
                + value.session().value() + " State=" + value.state() + " Target=" + value.target().resourceId()
                + " Execution=" + value.targetExecution().orElse("-")), false);
        value.failure().ifPresent(problem -> source.sendFailure(Component.literal(problem)));
        return value.failure().isEmpty() ? 1 : 0;
    }

    private static int failure(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message == null ? "DEV_SCENARIO_INVALID" : message));
        return 0;
    }
}
