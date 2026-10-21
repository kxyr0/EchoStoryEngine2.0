package io.github.kxyr0.echostoryengine.story.forge.diagnostic;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.core.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echostoryengine.story.forge.extension.catalog.ExtensionCatalog;
import io.github.kxyr0.echostoryengine.story.forge.host.readmodel.HostHealth;
import io.github.kxyr0.echostoryengine.story.forge.host.readmodel.HostSnapshot;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.StoryCatalogState;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Generic Brigadier tree for `/echo_story`; production supplies a Forge command surface.
 */
public final class EchoStoryCommandTree<S> {
    public static final int MAX_LIMIT = 50;
    private static final int DEFAULT_LIMIT = 10;
    private static final ForgeDiagnosticFormatter FORMATTER = new ForgeDiagnosticFormatter();

    private final InMemoryDiagnosticBuffer buffer;
    private final Supplier<Optional<HostSnapshot>> hostSnapshot;
    private final StoryCatalogState catalogState;
    private final Supplier<ExtensionCatalog> extensionCatalog;
    private final CommandSurface<S> surface;

    public EchoStoryCommandTree(
            InMemoryDiagnosticBuffer buffer,
            Supplier<Optional<HostSnapshot>> hostSnapshot,
            StoryCatalogState catalogState,
            Supplier<ExtensionCatalog> extensionCatalog,
            CommandSurface<S> surface) {
        this.buffer = Objects.requireNonNull(buffer, "buffer");
        this.hostSnapshot = Objects.requireNonNull(hostSnapshot, "hostSnapshot");
        this.catalogState = Objects.requireNonNull(catalogState, "catalogState");
        this.extensionCatalog = Objects.requireNonNull(extensionCatalog, "extensionCatalog");
        this.surface = Objects.requireNonNull(surface, "surface");
    }

    public void register(CommandDispatcher<S> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<S>literal("echo_story")
                .requires(surface::hasPermission)
                .then(LiteralArgumentBuilder.<S>literal("health").executes(this::sendHealth))
                .then(LiteralArgumentBuilder.<S>literal("list").executes(this::sendAllStories))
                .then(LiteralArgumentBuilder.<S>literal("stories").executes(this::sendStories))
                .then(LiteralArgumentBuilder.<S>literal("executions").executes(this::sendExecutions))
                .then(execution())
                .then(LiteralArgumentBuilder.<S>literal("extensions").executes(this::sendExtensions))
                .then(extension())
                .then(diagnostics()));
    }

    private LiteralArgumentBuilder<S> execution() {
        return LiteralArgumentBuilder.<S>literal("execution")
                .then(RequiredArgumentBuilder.<S, String>argument("executionId", StringArgumentType.greedyString())
                        .executes(this::sendExecution));
    }

    private LiteralArgumentBuilder<S> diagnostics() {
        return LiteralArgumentBuilder.<S>literal("diagnostics")
                .executes(context -> sendDiagnostics(context, DEFAULT_LIMIT))
                .then(RequiredArgumentBuilder.<S, String>argument("limit", StringArgumentType.greedyString())
                        .executes(this::sendDiagnosticsWithLimit));
    }

    private LiteralArgumentBuilder<S> extension() {
        return LiteralArgumentBuilder.<S>literal("extension")
                .then(RequiredArgumentBuilder.<S, String>argument("extensionId", StringArgumentType.greedyString())
                        .executes(this::sendExtension));
    }

    private int sendHealth(CommandContext<S> context) {
        HostSnapshot snapshot = currentSnapshot();
        return sendLines(context, FORMATTER.health(
                snapshot.health(),
                catalogState.current(),
                recentWarningOrErrorCount()));
    }

    private int sendStories(CommandContext<S> context) {
        return sendLines(context, FORMATTER.stories(catalogState.current(), MAX_LIMIT));
    }

    private int sendAllStories(CommandContext<S> context) {
        return sendLines(context, FORMATTER.stories(catalogState.current(), Integer.MAX_VALUE));
    }

    private int sendExecutions(CommandContext<S> context) {
        return sendLines(context, FORMATTER.executions(currentSnapshot().executions(), MAX_LIMIT));
    }

    private int sendExecution(CommandContext<S> context) {
        String raw = StringArgumentType.getString(context, "executionId");
        try {
            ExecutionId executionId = ExecutionId.of(raw);
            Optional<io.github.kxyr0.echostoryengine.story.forge.host.readmodel.ExecutionSummary> summary =
                    currentSnapshot().findExecution(executionId);
            return sendLines(context, FORMATTER.execution(executionId, summary));
        } catch (IllegalArgumentException exception) {
            return sendLines(context, List.of("ID execution должен иметь формат namespace:path."));
        }
    }

    private int sendExtensions(CommandContext<S> context) {
        return sendLines(context, FORMATTER.extensions(extensionCatalog.get().summaries(), MAX_LIMIT));
    }

    private int sendExtension(CommandContext<S> context) {
        String raw = StringArgumentType.getString(context, "extensionId");
        try {
            NamespacedId extensionId = NamespacedId.of(raw);
            return sendLines(context, FORMATTER.extension(extensionId, extensionCatalog.get().detail(extensionId)));
        } catch (IllegalArgumentException exception) {
            return sendLines(context, List.of("ID extension должен иметь формат namespace:path."));
        }
    }

    private int sendDiagnosticsWithLimit(CommandContext<S> context) {
        Optional<Integer> limit = parseLimit(StringArgumentType.getString(context, "limit"));
        if (limit.isEmpty()) {
            return sendLines(context, List.of("Лимит должен быть целым числом от 1 до " + MAX_LIMIT + "."));
        }
        return sendDiagnostics(context, limit.orElseThrow());
    }

    private int sendDiagnostics(CommandContext<S> context, int limit) {
        return sendLines(context, FORMATTER.diagnostics(buffer.recent(), limit));
    }

    private Optional<Integer> parseLimit(String raw) {
        try {
            int parsed = Integer.parseInt(raw);
            if (parsed < 1 || parsed > MAX_LIMIT) {
                return Optional.empty();
            }
            return Optional.of(parsed);
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private int sendLines(CommandContext<S> context, List<String> lines) {
        lines.forEach(line -> surface.send(context.getSource(), line));
        return lines.size();
    }

    private HostSnapshot currentSnapshot() {
        return hostSnapshot.get().orElseGet(() -> new HostSnapshot(inactiveHealth(), List.of()));
    }

    private HostHealth inactiveHealth() {
        return new HostHealth(
                io.github.kxyr0.echostoryengine.story.forge.host.ForgeServerHostState.NOT_STARTED,
                catalogState.current().revision(),
                catalogState.current().storyCount(),
                catalogState.latestReload(),
                0,
                0,
                0,
                0,
                0,
                0);
    }

    private int recentWarningOrErrorCount() {
        return (int) buffer.recent().stream()
                .filter(diagnostic -> diagnostic.severity() == DiagnosticSeverity.WARN
                        || diagnostic.severity() == DiagnosticSeverity.ERROR)
                .count();
    }

    public interface CommandSurface<S> {
        boolean hasPermission(S source);

        void send(S source, String line);
    }
}
