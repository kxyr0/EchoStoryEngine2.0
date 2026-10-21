package io.github.kxyr0.echodialogue.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.kxyr0.echodialogue.core.session.DialogueTransition;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogState;
import io.github.kxyr0.echodialogue.forge.diagnostic.ForgeDiagnosticFormatter;
import io.github.kxyr0.echodialogue.forge.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echodialogue.forge.host.ForgeDialogueHost;
import io.github.kxyr0.echodialogue.forge.status.ForgeDialogueHostStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * `/echo_dialogue` command tree for D1 authoring and diagnostics.
 */
public final class EchoDialogueCommands {
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final DialogueCatalogState catalogState;
    private final InMemoryDiagnosticBuffer buffer;
    private final ForgeDiagnosticFormatter formatter = new ForgeDiagnosticFormatter();
    private final DialoguePlayTargetResolver playTargets = new DialoguePlayTargetResolver();
    private final java.util.function.Supplier<Optional<ForgeDialogueHost>> host;

    public EchoDialogueCommands(
            DialogueCatalogState catalogState,
            InMemoryDiagnosticBuffer buffer,
            java.util.function.Supplier<Optional<ForgeDialogueHost>> host
    ) {
        this.catalogState = catalogState;
        this.buffer = buffer;
        this.host = host;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_dialogue")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("health").executes(context -> health(context.getSource())))
                .then(Commands.literal("list").executes(context -> dialogues(context.getSource())))
                .then(Commands.literal("dialogues").executes(context -> dialogues(context.getSource())))
                .then(Commands.literal("active").executes(context -> active(context.getSource())))
                .then(Commands.literal("cancel").executes(context -> cancel(context.getSource())))
                .then(Commands.literal("providers").executes(context -> providers(context.getSource())))
                .then(Commands.literal("provider")
                        .then(Commands.argument("providerId", ResourceLocationArgument.id())
                                .executes(context -> provider(
                                        context.getSource(),
                                        ResourceLocationArgument.getId(context, "providerId").toString()))))
                .then(Commands.literal("diagnostics")
                        .executes(context -> diagnostics(context.getSource(), DEFAULT_LIMIT))
                        .then(Commands.argument("limit", IntegerArgumentType.integer(1, MAX_LIMIT))
                                .executes(context -> diagnostics(
                                        context.getSource(), IntegerArgumentType.getInteger(context, "limit")))))
                .then(Commands.literal("play")
                        .then(Commands.argument("dialogue", ResourceLocationArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        playTargets.suggestions(catalogState.current()), builder))
                                .executes(context -> play(
                                        context.getSource(),
                                        ResourceLocationArgument.getId(context, "dialogue").toString(),
                                        context.getSource().getPlayer()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> play(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "dialogue").toString(),
                                                EntityArgument.getPlayer(context, "player")))))));
    }

    private int health(CommandSourceStack source) {
        Optional<ForgeDialogueHostStatus> status = host.get().map(ForgeDialogueHost::status);
        List<String> lines = new ArrayList<>(formatter.health(
                catalogState.current(), status.map(ForgeDialogueHostStatus::activeTotal).orElse(0)));
        status.ifPresent(value -> lines.add("Удержано terminal records: " + value.retainedTerminalTotal()));
        status.ifPresent(value -> lines.add("Заблокировано/повреждено snapshots при recovery: "
                + (value.blockedTotal() + value.corruptSnapshots())));
        return send(source, lines);
    }

    private int dialogues(CommandSourceStack source) {
        List<String> ids = catalogState.current().sortedIds();
        if (ids.isEmpty()) {
            return send(source, List.of("Каталог диалогов пуст."));
        }
        return send(source, ids.stream().map(id -> "Диалог: " + id).toList());
    }

    private int active(CommandSourceStack source) {
        return send(source, host.get()
                .map(ForgeDialogueHost::status)
                .map(this::activeStatus)
                .orElseGet(() -> List.of("Dialogue host ещё не запущен.")));
    }

    private int cancel(CommandSourceStack source) {
        Optional<ForgeDialogueHost> current = host.get();
        if (current.isEmpty()) {
            return send(source, List.of("Dialogue host ещё не запущен."));
        }
        ServerPlayer player = source.getPlayer();
        Optional<DialogueTransition> transition = current.orElseThrow().cancel(player);
        return send(source, transition.map(value -> List.of("Сессия отменена: " + value.status()))
                .orElseGet(() -> List.of("У игрока нет активной сессии.")));
    }

    private int providers(CommandSourceStack source) {
        return send(source, host.get()
                .map(ForgeDialogueHost::providerViews)
                .map(EchoDialogueProviderCommandLines::providers)
                .orElseGet(() -> List.of("Dialogue host ещё не запущен.")));
    }

    private int provider(CommandSourceStack source, String providerId) {
        return send(source, host.get()
                .map(ForgeDialogueHost::providerViews)
                .map(views -> EchoDialogueProviderCommandLines.provider(views, providerId))
                .orElseGet(() -> List.of("Dialogue host ещё не запущен.")));
    }

    private int diagnostics(CommandSourceStack source, int limit) {
        return send(source, formatter.diagnostics(buffer.recent(), limit));
    }

    private int play(CommandSourceStack source, String rawDialogueId, ServerPlayer player) {
        Optional<ForgeDialogueHost> current = host.get();
        if (current.isEmpty()) {
            return send(source, List.of("Dialogue host ещё не запущен."));
        }
        DialoguePlayTargetResolver.Resolution resolution = playTargets.resolve(catalogState.current(), rawDialogueId);
        if (resolution.ambiguous()) {
            return send(source, List.of("Диалог по имени файла неоднозначен: " + rawDialogueId,
                    "Совпадения: " + resolution.ambiguousMatches()));
        }
        if (!resolution.resolved()) {
            return send(source, List.of("Диалог не найден по ID или имени файла: " + rawDialogueId));
        }
        DialogueTransition transition = current.orElseThrow().start(player, resolution.dialogueId());
        return send(source, List.of("Результат запуска: " + transition.status()));
    }

    private int send(CommandSourceStack source, List<String> lines) {
        lines.forEach(line -> source.sendSuccess(() -> Component.literal(line), false));
        return lines.size();
    }

    private List<String> activeStatus(ForgeDialogueHostStatus status) {
        return List.of(
                "ACTIVE attached: " + status.activeAttached(),
                "ACTIVE detached/offline: " + status.activeDetached(),
                "COMPLETED удержано: " + status.retainedCompleted(),
                "CANCELLED удержано: " + status.retainedCancelled(),
                "BLOCKED missing definition: " + status.blockedMissingDefinition(),
                "BLOCKED incompatible fingerprint: " + status.blockedIncompatibleFingerprint(),
                "BLOCKED duplicate active audience: " + status.duplicateActiveAudienceConflicts(),
                "CORRUPT/failed load: " + status.corruptSnapshots(),
                "Persistence queue: operations=" + status.queuedPersistenceOperations()
                        + ", completions=" + status.queuedPersistenceCompletions()
                        + ", dropped=" + status.droppedPersistenceCompletions());
    }
}
