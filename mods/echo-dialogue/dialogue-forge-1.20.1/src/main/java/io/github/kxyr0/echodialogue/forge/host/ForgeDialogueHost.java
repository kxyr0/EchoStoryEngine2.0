package io.github.kxyr0.echodialogue.forge.host;

import io.github.kxyr0.echodialogue.api.choice.ChoiceId;
import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.core.host.ActiveDialogueSessions;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryRevision;
import io.github.kxyr0.echodialogue.core.persistence.DialogueSessionRepository;
import io.github.kxyr0.echodialogue.core.session.DialogueInput;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionEngine;
import io.github.kxyr0.echodialogue.core.session.DialogueTransition;
import io.github.kxyr0.echodialogue.core.session.DialogueTransitionStatus;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogState;
import io.github.kxyr0.echodialogue.forge.diagnostic.ForgeDiagnosticSink;
import io.github.kxyr0.echodialogue.forge.persistence.ForgePersistenceScheduler;
import io.github.kxyr0.echodialogue.forge.presentation.ForgeSpeakerPresentationCatalog;
import io.github.kxyr0.echodialogue.forge.presentation.ForgeSpeakerPresentationProviderView;
import io.github.kxyr0.echodialogue.forge.service.DialoguePublicSessionReadModel;
import io.github.kxyr0.echodialogue.forge.status.ForgeDialogueHostStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.LongSupplier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-thread owner of durable standalone dialogue sessions.
 */
public final class ForgeDialogueHost {
    private static final java.time.Duration SHUTDOWN_TIMEOUT = java.time.Duration.ofSeconds(5);
    private static final int MAX_COMPLETIONS_PER_TICK = 32;

    final DialogueCatalogState catalogState;
    private final ForgeDiagnosticSink diagnosticSink;
    private final ForgePersistenceScheduler scheduler;
    private final ForgeSpeakerPresentationCatalog presentationCatalog;
    private final DialogueSessionEngine engine = new DialogueSessionEngine();
    final ActiveDialogueSessions sessions = new ActiveDialogueSessions(engine);
    final Map<DialogueSessionId, DialogueRepositoryRevision> repositoryRevisions = new HashMap<>();
    private final ForgeDialogueStatusTracker statusTracker = new ForgeDialogueStatusTracker();
    private final ForgeDialoguePublicService publicService;
    final Function<AudienceId, Optional<ServerPlayer>> players;
    private final ForgeDialogueRecovery recovery;
    final ForgeDialogueSaveFlow saveFlow;
    boolean recovered;
    boolean stopped;

    public ForgeDialogueHost(DialogueCatalogState catalogState, ForgeDiagnosticSink diagnosticSink,
            DialogueSessionRepository repository, ForgePersistenceScheduler scheduler) {
        this(catalogState, diagnosticSink, repository, scheduler, System::currentTimeMillis, id -> Optional.empty(),
                new ForgeSpeakerPresentationCatalog(List.of(), diagnosticSink));
    }

    public ForgeDialogueHost(DialogueCatalogState catalogState, ForgeDiagnosticSink diagnosticSink,
            DialogueSessionRepository repository, ForgePersistenceScheduler scheduler, LongSupplier clock,
            Function<AudienceId, Optional<ServerPlayer>> players, ForgeSpeakerPresentationCatalog presentationCatalog) {
        this.catalogState = catalogState;
        this.diagnosticSink = diagnosticSink;
        this.scheduler = scheduler;
        this.players = java.util.Objects.requireNonNull(players, "players");
        this.presentationCatalog = java.util.Objects.requireNonNull(presentationCatalog, "presentationCatalog");
        DialoguePublicSessionReadModel readModel = new DialoguePublicSessionReadModel(diagnosticSink);
        this.publicService = new ForgeDialoguePublicService(this, readModel);
        ForgeDialogueCheckpointFactory checkpoints = new ForgeDialogueCheckpointFactory(clock);
        this.recovery = new ForgeDialogueRecovery(catalogState, diagnosticSink, sessions, engine, repositoryRevisions,
                statusTracker, readModel, presentationCatalog);
        this.saveFlow = new ForgeDialogueSaveFlow(diagnosticSink, repository, scheduler, sessions, engine,
                repositoryRevisions, checkpoints, statusTracker, readModel, presentationCatalog,
                id -> catalogState.current().find(id));
        diagnosticSink.accept(DialogueDiagnostic.info("EDLG-FORGE-006",
                DiagnosticCategory.HOST_LIFECYCLE, "Dialogue host активен."));
        queueRecovery(repository);
    }

    public DialogueTransition start(ServerPlayer player, DialogueId dialogueId) {
        if (stopped) {
            DialogueTransition transition = ForgeDialogueFailures.stopped(dialogueId, audience(player));
            saveFlow.deliver(Optional.of(player), transition);
            return transition;
        }
        statusTracker.playerAttached(audience(player));
        Optional<CompiledDialogue> dialogue = catalogState.current().find(dialogueId);
        if (dialogue.isPresent()) { return startFound(player, dialogue.orElseThrow()); }
        DialogueTransition transition = ForgeDialogueFailures.missing(dialogueId, audience(player));
        saveFlow.deliver(Optional.of(player), transition);
        return transition;
    }

    public Optional<DialogueTransition> advance(ServerPlayer player, String sessionId, int expectedRevision) {
        if (stopped) { return Optional.empty(); }
        Optional<DialogueTransition> transition = sessions.preview(
                audience(player), DialogueInput.advance(new DialogueSessionId(sessionId), expectedRevision));
        transition.ifPresent(value -> saveFlow.saveOrDeliver(Optional.of(player), audience(player), value));
        return transition;
    }

    public Optional<DialogueTransition> selectChoice(ServerPlayer player, String sessionId, int expectedRevision,
            String choiceId) {
        if (stopped) { return Optional.empty(); }
        try {
            Optional<DialogueTransition> transition = sessions.preview(audience(player), DialogueInput.selectChoice(
                    new DialogueSessionId(sessionId), expectedRevision, new ChoiceId(choiceId)));
            transition.ifPresent(value -> saveFlow.saveOrDeliver(Optional.of(player), audience(player), value));
            return transition;
        } catch (IllegalArgumentException exception) {
            diagnosticSink.accept(DialogueDiagnostic.error("EDLG-FORGE-022",
                    DiagnosticCategory.UNKNOWN_CHOICE_ID, "Некорректный ChoiceId в network request."));
            return Optional.empty();
        }
    }

    public Optional<DialogueTransition> cancel(ServerPlayer player) {
        if (stopped) { return Optional.empty(); }
        Optional<DialogueTransition> transition = sessions.stateFor(audience(player)).flatMap(state ->
                sessions.preview(audience(player), DialogueInput.cancel(state.sessionId(), state.revision())));
        transition.ifPresent(value -> saveFlow.saveOrDeliver(Optional.of(player), audience(player), value));
        return transition;
    }

    public void disconnect(ServerPlayer player) {
        AudienceId audienceId = audience(player);
        statusTracker.playerDetached(audienceId);
        if (sessions.stateFor(audienceId).isEmpty()) { return; }
        diagnosticSink.accept(DialogueDiagnostic.info("EDLG-FORGE-009",
                DiagnosticCategory.ACTIVE_SESSION_WAITING_FOR_PLAYER,
                    "Игрок отключён; durable ACTIVE session сохранена без отмены."));
    }

    public void reattach(ServerPlayer player) {
        AudienceId audienceId = audience(player);
        statusTracker.playerAttached(audienceId); recovery.reattach(player, audienceId);
    }


    public int activeCount() { return sessions.activeCount(); }

    public ForgeDialogueHostStatus status() { return statusTracker.status(sessions, scheduler); }

    public List<ForgeSpeakerPresentationProviderView> providerViews() { return presentationCatalog.views(); }

    public DialogueService service() { return publicService; }

    public int drainPersistenceCompletions() { return scheduler.drainCompletions(MAX_COMPLETIONS_PER_TICK); }

    public void shutdown() {
        if (stopped) { return; }
        stopped = true;
        drainPersistenceCompletions();
        if (!scheduler.shutdown(SHUTDOWN_TIMEOUT)) {
            diagnosticSink.accept(DialogueDiagnostic.error("EDLG-FORGE-011",
                    DiagnosticCategory.SHUTDOWN_FLUSH_TIMED_OUT,
                    "Persistence scheduler не завершился за bounded timeout."));
        }
        drainAllPersistenceCompletions();
        presentationCatalog.close();
        sessions.clear();
        diagnosticSink.accept(DialogueDiagnostic.info("EDLG-FORGE-007",
                DiagnosticCategory.HOST_LIFECYCLE, "Dialogue host остановлен; сессии очищены."));
    }

    private DialogueTransition startFound(ServerPlayer player, CompiledDialogue dialogue) {
        DialogueSessionId session = new DialogueSessionId(UUID.randomUUID().toString());
        DialogueTransition transition = sessions.previewStart(audience(player), session, dialogue);
        DialogueTransitionStatus status = transition.status();
        if (io.github.kxyr0.echodialogue.forge.domain.DialogueCommitPolicy.initial(status)) {
            saveFlow.queueInitialSave(Optional.of(player), audience(player), dialogue, transition);
        } else {
            saveFlow.deliver(Optional.of(player), transition);
        }
        return transition;
    }

    private void queueRecovery(DialogueSessionRepository repository) {
        ForgePersistenceScheduler.SubmitResult result = scheduler.submit(() -> recovery.loadAll(repository), loaded -> {
            reattachRestoredPlayers(recovery.recover(loaded));
            saveFlow.domainEvents.recover(loaded);
            recovered = true;
            recovery.dispatchRecoveredExternalWaits();
        });
        if (result != ForgePersistenceScheduler.SubmitResult.ACCEPTED) {
            diagnosticSink.accept(DialogueDiagnostic.error("EDLG-FORGE-012",
                    DiagnosticCategory.PERSISTENCE_QUEUE_SATURATED,
                    "Persistence queue не приняла recovery load."));
        }
    }

    private void reattachRestoredPlayers(List<AudienceId> audienceIds) {
        audienceIds.forEach(id -> players.apply(id).ifPresent(player -> recovery.reattach(player, id)));
    }

    private void drainAllPersistenceCompletions() {
        int drained = drainPersistenceCompletions();
        while (drained == MAX_COMPLETIONS_PER_TICK) { drained = drainPersistenceCompletions(); }
    }

    private AudienceId audience(ServerPlayer player) { return new AudienceId(player.getUUID().toString()); }

}
