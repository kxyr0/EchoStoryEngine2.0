package io.github.kxyr0.echodialogue.forge.host;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.core.host.ActiveDialogueSessions;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryRevision;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryWriteResult;
import io.github.kxyr0.echodialogue.core.persistence.DialogueSessionRepository;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionEngine;
import io.github.kxyr0.echodialogue.core.session.DialogueTransition;
import io.github.kxyr0.echodialogue.core.session.DialogueTransitionStatus;
import io.github.kxyr0.echodialogue.forge.diagnostic.ForgeDiagnosticSink;
import io.github.kxyr0.echodialogue.forge.api.client.DialogueCloseReason;
import io.github.kxyr0.echodialogue.forge.host.wait.ForgeDialogueExternalWaitDispatcher;
import io.github.kxyr0.echodialogue.forge.network.DialogueNetwork;
import io.github.kxyr0.echodialogue.forge.persistence.ForgePersistenceScheduler;
import io.github.kxyr0.echodialogue.forge.presentation.ForgeSpeakerPresentationCatalog;
import io.github.kxyr0.echodialogue.forge.service.DialoguePublicSessionReadModel;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;

/**
 * Coordinates durable save completion before network presentation.
 */
final class ForgeDialogueSaveFlow {
    final io.github.kxyr0.echodialogue.forge.domain.DialogueEventOrigins domainEvents =
            new io.github.kxyr0.echodialogue.forge.domain.DialogueEventOrigins();
    private final ForgeDiagnosticSink diagnosticSink;
    private final DialogueSessionRepository repository;
    private final ForgePersistenceScheduler scheduler;
    private final ActiveDialogueSessions sessions;
    private final DialogueSessionEngine engine;
    private final Map<DialogueSessionId, DialogueRepositoryRevision> repositoryRevisions;
    private final ForgeDialogueCheckpointFactory checkpoints;
    private final ForgeDialogueStatusTracker statusTracker;
    private final DialoguePublicSessionReadModel readModel;
    private final ForgeSpeakerPresentationCatalog presentationCatalog;
    private final Function<DialogueId, Optional<CompiledDialogue>> dialogueResolver;
    private final ForgeDialogueExternalWaitDispatcher externalWaits;

    ForgeDialogueSaveFlow(ForgeDiagnosticSink diagnosticSink, DialogueSessionRepository repository,
            ForgePersistenceScheduler scheduler, ActiveDialogueSessions sessions, DialogueSessionEngine engine,
            Map<DialogueSessionId, DialogueRepositoryRevision> repositoryRevisions,
            ForgeDialogueCheckpointFactory checkpoints, ForgeDialogueStatusTracker statusTracker,
            DialoguePublicSessionReadModel readModel, ForgeSpeakerPresentationCatalog presentationCatalog,
            Function<DialogueId, Optional<CompiledDialogue>> dialogueResolver) {
        this.diagnosticSink = diagnosticSink;
        this.repository = repository;
        this.scheduler = scheduler;
        this.sessions = sessions;
        this.engine = engine;
        this.repositoryRevisions = repositoryRevisions;
        this.checkpoints = checkpoints;
        this.statusTracker = statusTracker;
        this.readModel = readModel;
        this.presentationCatalog = presentationCatalog;
        this.dialogueResolver = dialogueResolver;
        this.externalWaits = new ForgeDialogueExternalWaitDispatcher(readModel);
    }

    void saveOrDeliver(Optional<ServerPlayer> player, AudienceId audienceId, DialogueTransition transition) {
        if (transition.status() == DialogueTransitionStatus.LOAD_REQUESTED) {
            loadOrReject(player, audienceId, transition);
            return;
        }
        if (io.github.kxyr0.echodialogue.forge.domain.DialogueCommitPolicy.requiresSave(transition.status())) {
            sessions.dialogueFor(audienceId).ifPresent(dialogue -> queueSave(player, audienceId, dialogue, transition,
                    expectedRevision(transition.state().sessionId())));
        } else {
            deliver(player, transition);
        }
    }

    void queueInitialSave(Optional<ServerPlayer> player, AudienceId audienceId, CompiledDialogue dialogue,
            DialogueTransition transition) {
        queueSave(player, audienceId, dialogue, transition, DialogueSessionRepository.SaveExpectation.expectedEmpty());
    }

    private void loadOrReject(Optional<ServerPlayer> player, AudienceId audienceId, DialogueTransition transition) {
        DialogueId targetId = transition.targetDialogueId().orElseThrow();
        Optional<CompiledDialogue> target = dialogueResolver.apply(targetId);
        if (target.isEmpty()) {
            DialogueDiagnostic diagnostic = DialogueDiagnostic.error("EDLG-FORGE-023",
                    DiagnosticCategory.COMPILED_DIALOGUE_MISSING, "Dialogue.Load target not found: " + targetId + ".");
            deliver(player, DialogueTransition.of(transition.state(), DialogueTransitionStatus.REJECTED,
                    Optional.empty(), diagnostic));
            resync(player, audienceId);
            return;
        }
        CompiledDialogue dialogue = target.orElseThrow();
        DialogueTransition loaded = engine.load(dialogue, transition.state());
        queueSave(player, audienceId, dialogue, loaded, expectedRevision(transition.state().sessionId()));
    }

    void deliver(Optional<ServerPlayer> player, DialogueTransition transition) {
        transition.diagnostics().forEach(diagnosticSink::accept);
        player.ifPresent(serverPlayer -> transition.presentation()
                .map(presentationCatalog::resolve)
                .ifPresent(presentation -> DialogueNetwork.sendOpen(serverPlayer, presentation)));
        if (terminal(transition.status())) {
            player.ifPresent(serverPlayer -> DialogueNetwork.sendClose(serverPlayer,
                    transition.state().sessionId().toString(), transition.state().revision(),
                    transition.status() == DialogueTransitionStatus.COMPLETED
                            ? DialogueCloseReason.COMPLETED : transition.status() == DialogueTransitionStatus.FAILED
                                    ? DialogueCloseReason.FAILED : DialogueCloseReason.CANCELLED));
        }
    }

    private void queueSave(Optional<ServerPlayer> player, AudienceId audienceId, CompiledDialogue dialogue,
            DialogueTransition transition, DialogueSessionRepository.SaveExpectation expectation) {
        DialogueCheckpoint checkpoint = domainEvents.stamp(
                checkpoints.checkpoint(transition, dialogue), transition, expectation.revision().isEmpty());
        ForgePersistenceScheduler.SubmitResult result = scheduler.submit(
                () -> saveSafely(checkpoint, expectation),
                saved -> applySaveResult(player, audienceId, dialogue, transition, saved));
        if (result != ForgePersistenceScheduler.SubmitResult.ACCEPTED) {
            readModel.saveFailed(transition.state().sessionId());
            diagnosticSink.accept(DialogueDiagnostic.error("EDLG-FORGE-012",
                    DiagnosticCategory.PERSISTENCE_QUEUE_SATURATED,
                    "Persistence queue rejected save; presentation was not changed."));
            resync(player, audienceId);
        }
    }

    private DialogueRepositoryWriteResult saveSafely(DialogueCheckpoint checkpoint,
            DialogueSessionRepository.SaveExpectation expectation) {
        try {
            return repository.save(checkpoint, expectation);
        } catch (RuntimeException exception) {
            return DialogueRepositoryWriteResult.failed(List.of(DialogueDiagnostic.error("EDLG-FORGE-017",
                    DiagnosticCategory.SESSION_SAVE_FAILED, "Persistence save failed unexpectedly.")));
        }
    }

    private void applySaveResult(Optional<ServerPlayer> player, AudienceId audienceId, CompiledDialogue dialogue,
            DialogueTransition transition, DialogueRepositoryWriteResult result) {
        if (saved(result)) {
            sessions.install(dialogue, transition.state());
            DialogueCheckpoint saved = result.checkpoint().orElseThrow();
            repositoryRevisions.put(saved.sessionId(), saved.repositoryRevision());
            readModel.saveSucceeded(saved);
            if (terminal(transition.status())) {
                statusTracker.terminalRetained(saved);
            }
            io.github.kxyr0.echodialogue.forge.domain.DialogueDomainEvents.publish(saved, false);
            externalWaits.dispatch(player, transition, saved);
            deliver(player, transition);
        } else {
            readModel.saveFailed(transition.state().sessionId());
            result.diagnostics().forEach(diagnosticSink::accept);
            resync(player, audienceId);
        }
    }

    private void resync(Optional<ServerPlayer> player, AudienceId audienceId) {
        sessions.stateFor(audienceId)
                .flatMap(state -> sessions.dialogueFor(audienceId)
                        .flatMap(dialogue -> engine.presentation(dialogue, state)))
                .map(presentationCatalog::resolve)
                .ifPresent(presentation -> player.ifPresent(serverPlayer -> DialogueNetwork.sendOpen(
                        serverPlayer, presentation)));
        diagnosticSink.accept(DialogueDiagnostic.info("EDLG-FORGE-015",
                DiagnosticCategory.CLIENT_RESYNCHRONIZED_AFTER_SAVE_FAILURE,
                "Client presentation was resynchronized to durable authoritative state."));
    }

    private boolean saved(DialogueRepositoryWriteResult result) {
        return result.kind() == DialogueRepositoryWriteResult.Kind.CREATED
                || result.kind() == DialogueRepositoryWriteResult.Kind.UPDATED;
    }

    private DialogueSessionRepository.SaveExpectation expectedRevision(DialogueSessionId sessionId) {
        return DialogueSessionRepository.SaveExpectation.expectedRevision(repositoryRevisions.getOrDefault(
                sessionId, new DialogueRepositoryRevision(0)));
    }

    private boolean terminal(DialogueTransitionStatus status) {
        return status == DialogueTransitionStatus.COMPLETED
                || status == DialogueTransitionStatus.CANCELLED || status == DialogueTransitionStatus.FAILED;
    }

}
