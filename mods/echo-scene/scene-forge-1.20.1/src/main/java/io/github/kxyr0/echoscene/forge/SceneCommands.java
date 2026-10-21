package io.github.kxyr0.echoscene.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echoscene.api.diagnostic.SceneDiagnostic;
import io.github.kxyr0.echoscene.compile.SceneCompileResult;
import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

final class SceneCommands {
    private SceneCommands() { }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher, SceneForgeCatalog catalog,
            SceneForgeRuntime runtime) {
        dispatcher.register(Commands.literal("echo_scene").requires(source -> source.hasPermission(2))
                .then(Commands.literal("list").executes(context -> list(context.getSource(), catalog)))
                .then(Commands.literal("inspect").then(Commands.argument("id", StringArgumentType.string())
                        .executes(context -> inspect(context.getSource(), catalog,
                                StringArgumentType.getString(context, "id")))))
                .then(Commands.literal("play").then(Commands.argument("id", StringArgumentType.string())
                        .executes(context -> play(context.getSource(), catalog, runtime,
                                StringArgumentType.getString(context, "id")))))
                .then(Commands.literal("running").executes(context -> running(context.getSource(), runtime)))
                .then(Commands.literal("cancel").then(Commands.argument("execution", StringArgumentType.string())
                        .executes(context -> cancel(context.getSource(), runtime,
                                StringArgumentType.getString(context, "execution"))))));
    }

    private static int list(CommandSourceStack source, SceneForgeCatalog catalog) {
        source.sendSuccess(() -> Component.literal("Echo Scene resources: " + catalog.list().size()), false);
        catalog.list().forEach(result -> source.sendSuccess(() -> Component.literal(label(result)), false));
        return catalog.list().size();
    }

    private static int inspect(CommandSourceStack source, SceneForgeCatalog catalog, String id) {
        SceneCompileResult result = catalog.find(id).orElse(null);
        if (result == null) return message(source, "Scene not found: " + id);
        if (result.definition().isPresent()) {
            var scene = result.definition().orElseThrow();
            source.sendSuccess(() -> Component.literal("resource=" + scene.resourceId().value()
                    + " entry=" + scene.entry().value() + " stages=" + scene.stages().size()
                    + " capabilities=" + scene.capabilityPlan()), false);
        }
        result.diagnostics().forEach(value -> source.sendSuccess(() -> Component.literal(value.severity() + " "
                + value.code() + " " + value.span().start().line() + ":" + value.span().start().column()
                + " " + value.message()), false));
        return result.successful() ? 1 : 0;
    }

    private static int play(CommandSourceStack source, SceneForgeCatalog catalog, SceneForgeRuntime runtime,
            String id) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        SceneCompileResult result = catalog.find(id).orElse(null);
        if (result == null || !result.successful()) return message(source, "Scene is missing or invalid: " + id);
        var inspection = runtime.play(result.definition().orElseThrow(), source.getPlayerOrException());
        source.sendSuccess(() -> Component.literal("Started " + inspection.executionId().value()
                + " state=" + inspection.state()), false);
        return 1;
    }

    private static int running(CommandSourceStack source, SceneForgeRuntime runtime) {
        List<io.github.kxyr0.echoscene.api.model.SceneInspection> running = runtime.running();
        source.sendSuccess(() -> Component.literal("Running scenes: " + running.size()), false);
        running.forEach(value -> source.sendSuccess(() -> Component.literal(value.executionId().value() + " "
                + value.resourceId().value() + " " + value.state()), false));
        return running.size();
    }

    private static int cancel(CommandSourceStack source, SceneForgeRuntime runtime, String id) {
        SceneExecutionId executionId = new SceneExecutionId(id);
        if (runtime.inspect(executionId).isEmpty()) return message(source, "Scene execution not found: " + id);
        var inspection = runtime.cancel(executionId);
        source.sendSuccess(() -> Component.literal("Cancelled " + inspection.executionId().value()), false);
        return 1;
    }

    private static String label(SceneCompileResult result) {
        String id = result.definition().map(value -> value.resourceId().value()).orElse("<invalid>");
        long errors = result.diagnostics().stream().filter(value -> value.severity() == SceneDiagnostic.Severity.ERROR)
                .count();
        return id + (errors == 0 ? " [VALID]" : " [ERRORS=" + errors + "]");
    }

    private static int message(CommandSourceStack source, String value) {
        source.sendFailure(Component.literal(value));
        return 0;
    }
}
