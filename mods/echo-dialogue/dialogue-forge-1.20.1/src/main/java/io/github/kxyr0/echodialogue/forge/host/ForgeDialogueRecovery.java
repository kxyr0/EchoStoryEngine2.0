package io.github.kxyr0.echodialogue.forge.host;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.core.host.ActiveDialogueSessions;
import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryLoadResult;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryRevision;
import io.github.kxyr0.echodialogue.core.persistence.DialogueSessionRepository;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionEngine;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionState;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogState;
import io.github.kxyr0.echodialogue.forge.diagnostic.ForgeDiagnosticSink;
import io.github.kxyr0.echodialogue.forge.api.wait.DialogueExternalWaitDispatchEvent;
import io.github.kxyr0.echodialogue.forge.network.DialogueNetwork;
import io.github.kxyr0.echodialogue.forge.presentation.ForgeSpeakerPresentationCatalog;
import io.github.kxyr0.echodialogue.forge.service.DialoguePublicSessionReadModel;
import io.github.kxyr0.echodialogue.persistence.recovery.DialogueRecoveryPlan;
import io.github.kxyr0.echodialogue.persistence.recovery.DialogueRecoveryPlanner;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

/**
 * Restores durable snapshots and reattaches available players.
 */
final class ForgeDialogueRecovery {
    private final DialogueCatalogState catalogState;
    private final ForgeDiagnosticSink diagnosticSink;
    private final ActiveDialogueSessions sessions;
    private final DialogueSessionEngine engine;
    private final Map<DialogueSessionId, DialogueRepositoryRevision> repositoryRevisions;
    private final ForgeDialogueStatusTracker statusTracker;
    private final DialoguePublicSessionReadModel readModel;
    private final ForgeSpeakerPresentationCatalog presentationCatalog;
    private final Consumer<DialogueExternalWaitDispatchEvent> externalWaitDispatcher;
    private final DialogueRecoveryPlanner planner = new DialogueRecoveryPlanner();

    ForgeDialogueRecovery(DialogueCatalogState catalogState, ForgeDiagnosticSink diagnosticSink,
            ActiveDialogueSessions sessions, DialogueSessionEngine engine,
            Map<DialogueSessionId, DialogueRepositoryRevision> repositoryRevisions,
            ForgeDialogueStatusTracker statusTracker, DialoguePublicSessionReadModel readModel,
            ForgeSpeakerPresentationCatalog presentationCatalog) {
        this(catalogState, diagnosticSink, sessions, engine, repositoryRevisions, statusTracker, readModel,
                presentationCatalog, event -> MinecraftForge.EVENT_BUS.post(event));
    }

    ForgeDialogueRecovery(DialogueCatalogState catalogState, ForgeDiagnosticSink diagnosticSink,
            ActiveDialogueSessions sessions, DialogueSessionEngine engine,
            Map<DialogueSessionId, DialogueRepositoryRevision> repositoryRevisions,
            ForgeDialogueStatusTracker statusTracker, DialoguePublicSessionReadModel readModel,
            ForgeSpeakerPresentationCatalog presentationCatalog,
            Consumer<DialogueExternalWaitDispatchEvent> externalWaitDispatcher) {
        this.catalogState = catalogState;
        this.diagnosticSink = diagnosticSink;
        this.sessions = sessions;
        this.engine = engine;
        this.repositoryRevisions = repositoryRevisions;
        this.statusTracker = statusTracker;
        this.readModel = readModel;
        this.presentationCatalog = presentationCatalog;
        this.externalWaitDispatcher = Objects.requireNonNull(externalWaitDispatcher, "externalWaitDispatcher");
    }

    ForgeDialogueRecovery(DialogueCatalogState catalogState, ForgeDiagnosticSink diagnosticSink,
            ActiveDialogueSessions sessions, DialogueSessionEngine engine,
            Map<DialogueSessionId, DialogueRepositoryRevision> repositoryRevisions,
            ForgeDialogueStatusTracker statusTracker,
            DialoguePublicSessionReadModel readModel) {
        this(catalogState, diagnosticSink, sessions, engine, repositoryRevisions, statusTracker, readModel,
                new ForgeSpeakerPresentationCatalog(List.of(), diagnosticSink));
    }

    List<DialogueRepositoryLoadResult> loadAll(DialogueSessionRepository repository) {
        try {
            return repository.loadAll();
        } catch (RuntimeException exception) {
            return List.of(DialogueRepositoryLoadResult.failed(List.of(DialogueDiagnostic.error("EDLG-FORGE-018",
                    DiagnosticCategory.SESSION_LOAD_FAILED,
                    "Recovery load завершился непредвиденно; snapshots сохранены."))));
        }
    }

    List<AudienceId> recover(List<DialogueRepositoryLoadResult> results) {
        statusTracker.resetRecoveredDurableState();
        readModel.resetRecoveredDurableState();
        List<AudienceId> restoredActiveAudiences = new ArrayList<>();
        results.stream()
                .sorted(Comparator.comparing(this::resultKey))
                .forEach(result -> recoverResult(result).ifPresent(restoredActiveAudiences::add));
        return List.copyOf(restoredActiveAudiences);
    }

    void reattach(ServerPlayer player, AudienceId audienceId) {
        sessions.stateFor(audienceId)
                .filter(state -> state.phase() == DialogueSessionPhase.ACTIVE)
                .flatMap(state -> sessions.dialogueFor(audienceId)
                        .flatMap(dialogue -> engine.presentation(dialogue, state)))
                .map(presentationCatalog::resolve)
                .ifPresent(presentation -> {
                    DialogueNetwork.sendOpen(player, presentation);
                    diagnosticSink.accept(DialogueDiagnostic.info("EDLG-FORGE-010",
                            DiagnosticCategory.PRESENTATION_REATTACHED,
                            "Presentation восстановлена для вернувшегося игрока."));
                });
    }

    void dispatchRecoveredExternalWaits() {
        sessions.activeAudienceIds().stream()
                .flatMap(audienceId -> sessions.stateFor(audienceId).stream())
                .filter(state -> state.phase() == DialogueSessionPhase.WAITING)
                .forEach(this::dispatchRecoveredExternalWait);
    }

    private Optional<AudienceId> recoverOne(DialogueCheckpoint checkpoint) {
        DialogueRecoveryPlan plan = planner.plan(checkpoint, catalogState.current().dialogues());
        DialogueRecoveryPlan.Kind kind = plan.kind();
        plan.diagnostics().forEach(diagnosticSink::accept);
        plan.diagnostics().forEach(statusTracker::blocked);
        if (kind == DialogueRecoveryPlan.Kind.ACTIVE_DETACHED) { return restoreActive(checkpoint); }
        if (kind == DialogueRecoveryPlan.Kind.TERMINAL) {
            statusTracker.terminalRetained(checkpoint);
            readModel.saveSucceeded(checkpoint);
            diagnosticSink.accept(DialogueDiagnostic.info("EDLG-FORGE-013",
                    DiagnosticCategory.TERMINAL_SESSION_RESTORED,
                    "Terminal dialogue session восстановлена и остаётся queryable."));
        }
        if (kind == DialogueRecoveryPlan.Kind.BLOCKED) { readModel.recoveredBlocked(checkpoint, plan.diagnostics()); }
        if (kind == DialogueRecoveryPlan.Kind.CORRUPT) { statusTracker.corruptSnapshot(); }
        return Optional.empty();
    }

    private Optional<AudienceId> restoreActive(DialogueCheckpoint checkpoint) {
        return catalogState.current().find(checkpoint.dialogueId()).flatMap(dialogue -> {
            if (sessions.stateFor(checkpoint.audienceId())
                    .filter(state -> state.phase() == DialogueSessionPhase.ACTIVE
                            || state.phase() == DialogueSessionPhase.WAITING)
                    .isPresent()) {
                statusTracker.duplicateActiveAudienceConflict();
                diagnosticSink.accept(DialogueDiagnostic.error("EDLG-FORGE-016",
                        DiagnosticCategory.DUPLICATE_ACTIVE_AUDIENCE_CONFLICT,
                        "Duplicate ACTIVE snapshot for one audience; first snapshot was restored."));
                return Optional.empty();
            }
            sessions.install(dialogue, state(checkpoint));
            repositoryRevisions.put(checkpoint.sessionId(), checkpoint.repositoryRevision());
            diagnosticSink.accept(DialogueDiagnostic.info("EDLG-FORGE-014",
                    DiagnosticCategory.ACTIVE_SESSION_RESTORED,
                    "ACTIVE dialogue session восстановлена detached."));
            return Optional.of(checkpoint.audienceId());
        });
    }

    private DialogueSessionState state(DialogueCheckpoint checkpoint) {
        return new DialogueSessionState(checkpoint.sessionId(), checkpoint.dialogueId(), checkpoint.audienceId(),
                checkpoint.currentNodeId(), checkpoint.lineOrdinal(), checkpoint.presentationKind(),
                checkpoint.phase(), checkpoint.revision(), checkpoint.terminalOutcomeId(), checkpoint.externalWait());
    }

    private void dispatchRecoveredExternalWait(DialogueSessionState state) {
        state.externalWait().ifPresent(wait -> {
            var repositoryRevision = repositoryRevisions.getOrDefault(state.sessionId(),
                    new DialogueRepositoryRevision(0));
            externalWaitDispatcher.accept(new DialogueExternalWaitDispatchEvent(Optional.empty(),
                    readModel.view(state, repositoryRevision), wait));
            diagnosticSink.accept(DialogueDiagnostic.info("EDLG-FORGE-024",
                    DiagnosticCategory.ACTIVE_SESSION_RESTORED,
                    "External wait восстановлена и отправлена adapter listeners."));
        });
    }

    private Optional<AudienceId> recoverResult(DialogueRepositoryLoadResult result) {
        result.diagnostics().forEach(diagnosticSink::accept);
        if (result.kind() == DialogueRepositoryLoadResult.Kind.FAILED) { statusTracker.corruptSnapshot(); }
        return result.checkpoint().flatMap(this::recoverOne);
    }

    private String resultKey(DialogueRepositoryLoadResult result) {
        return result.checkpoint().map(checkpoint -> checkpoint.sessionId().toString()).orElse(result.kind().name());
    }
}
