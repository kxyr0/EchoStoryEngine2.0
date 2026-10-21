package io.github.kxyr0.echodialogue.forge.host;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.outcome.DialogueOutcomeId;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionStatus;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echodialogue.core.fingerprint.CompiledDialogueFingerprinter;
import io.github.kxyr0.echodialogue.core.host.ActiveDialogueSessions;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryLoadResult;
import io.github.kxyr0.echodialogue.core.session.DialogueInput;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionEngine;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionState;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogCandidate;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogState;
import io.github.kxyr0.echodialogue.forge.api.wait.DialogueExternalWaitDispatchEvent;
import io.github.kxyr0.echodialogue.forge.diagnostic.ForgeDiagnosticSink;
import io.github.kxyr0.echodialogue.forge.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echodialogue.forge.persistence.ForgePersistenceScheduler;
import io.github.kxyr0.echodialogue.forge.presentation.ForgeSpeakerPresentationCatalog;
import io.github.kxyr0.echodialogue.forge.service.DialoguePublicSessionReadModel;
import io.github.kxyr0.echodialogue.forge.status.ForgeDialogueHostStatus;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ForgeDialogueRecoveryStatusTest {
    @Test
    void statusDistinguishesAttachedDetachedAndRetainedTerminal() {
        ActiveDialogueSessions sessions = new ActiveDialogueSessions(new DialogueSessionEngine());
        ForgeDialogueStatusTracker tracker = new ForgeDialogueStatusTracker();
        ForgePersistenceScheduler scheduler = new ForgePersistenceScheduler("Echo Dialogue Status Test");
        try {
            CompiledDialogue dialogue = dialogue();
            AudienceId audience = new AudienceId("audience-1");
            DialogueSessionState state = new DialogueSessionEngine()
                    .start(dialogue, new DialogueSessionId("session-1"), audience)
                    .state();

            sessions.install(dialogue, state);
            tracker.playerAttached(audience);
            assertEquals(1, tracker.status(sessions, scheduler).activeAttached());

            tracker.playerDetached(audience);
            tracker.terminalRetained(completedCheckpoint("terminal-1", new AudienceId("audience-2")));
            ForgeDialogueHostStatus status = tracker.status(sessions, scheduler);

            assertEquals(1, status.activeDetached());
            assertEquals(1, status.retainedCompleted());
        } finally {
            scheduler.shutdown(Duration.ofSeconds(1));
        }
    }

    @Test
    void duplicateActiveAudienceConflictKeepsFirstSessionDeterministically() {
        CompiledDialogue dialogue = dialogue();
        AudienceId audience = new AudienceId("audience-1");
        DialogueCatalogState catalogState = new DialogueCatalogState();
        catalogState.install(new DialogueCatalogCandidate(List.of(dialogue), List.of()));
        ActiveDialogueSessions sessions = new ActiveDialogueSessions(new DialogueSessionEngine());
        InMemoryDiagnosticBuffer buffer = new InMemoryDiagnosticBuffer(10);
        ForgeDiagnosticSink diagnosticSink = new ForgeDiagnosticSink(buffer);
        ForgeDialogueStatusTracker tracker = new ForgeDialogueStatusTracker();
        ForgeDialogueRecovery recovery = new ForgeDialogueRecovery(
                catalogState,
                diagnosticSink,
                sessions,
                new DialogueSessionEngine(),
                new HashMap<>(),
                tracker,
                new DialoguePublicSessionReadModel(diagnosticSink));

        DialogueCheckpoint second = activeCheckpoint("session-b", audience);
        DialogueCheckpoint first = activeCheckpoint("session-a", audience);
        ForgePersistenceScheduler scheduler = new ForgePersistenceScheduler("Echo Dialogue Empty Status");
        try {
            recovery.recover(List.of(
                    DialogueRepositoryLoadResult.found(second, List.of()),
                    DialogueRepositoryLoadResult.found(first, List.of())));

            assertEquals(new DialogueSessionId("session-a"), sessions.stateFor(audience).orElseThrow().sessionId());
            assertEquals(1, tracker.status(sessions, scheduler).duplicateActiveAudienceConflicts());
            assertTrue(buffer.recent().stream().anyMatch(diagnostic -> diagnostic.code().equals("EDLG-FORGE-016")));
        } finally {
            scheduler.shutdown(Duration.ofSeconds(1));
        }
    }

    @Test
    void recoveryReportsRestoredActiveAudiencesForLatePlayerReattach() {
        CompiledDialogue dialogue = dialogue();
        AudienceId audience = new AudienceId("audience-late-login");
        DialogueCatalogState catalogState = new DialogueCatalogState();
        catalogState.install(new DialogueCatalogCandidate(List.of(dialogue), List.of()));
        ActiveDialogueSessions sessions = new ActiveDialogueSessions(new DialogueSessionEngine());
        InMemoryDiagnosticBuffer buffer = new InMemoryDiagnosticBuffer(10);
        ForgeDiagnosticSink diagnosticSink = new ForgeDiagnosticSink(buffer);
        ForgeDialogueRecovery recovery = new ForgeDialogueRecovery(
                catalogState,
                diagnosticSink,
                sessions,
                new DialogueSessionEngine(),
                new HashMap<>(),
                new ForgeDialogueStatusTracker(),
                new DialoguePublicSessionReadModel(diagnosticSink));

        List<AudienceId> restored = recovery.recover(List.of(
                DialogueRepositoryLoadResult.found(activeCheckpoint("session-late-login", audience), List.of())));

        assertEquals(List.of(audience), restored);
        assertEquals(Optional.of(new DialogueSessionId("session-late-login")),
                sessions.stateFor(audience).map(DialogueSessionState::sessionId));
    }

    @Test
    void recoveryRedispatchesWaitingExternalWaitsAfterHostIsRecovered() {
        CompiledDialogue dialogue = dialogue();
        AudienceId audience = new AudienceId("audience-waiting");
        DialogueCatalogState catalogState = new DialogueCatalogState();
        catalogState.install(new DialogueCatalogCandidate(List.of(dialogue), List.of()));
        ActiveDialogueSessions sessions = new ActiveDialogueSessions(new DialogueSessionEngine());
        InMemoryDiagnosticBuffer buffer = new InMemoryDiagnosticBuffer(10);
        ForgeDiagnosticSink diagnosticSink = new ForgeDiagnosticSink(buffer);
        List<DialogueExternalWaitDispatchEvent> events = new ArrayList<>();
        ForgeDialogueRecovery recovery = new ForgeDialogueRecovery(
                catalogState,
                diagnosticSink,
                sessions,
                new DialogueSessionEngine(),
                new HashMap<>(),
                new ForgeDialogueStatusTracker(),
                new DialoguePublicSessionReadModel(diagnosticSink),
                new ForgeSpeakerPresentationCatalog(List.of(), diagnosticSink),
                events::add);

        DialogueExternalWait wait = externalWait();
        recovery.recover(List.of(DialogueRepositoryLoadResult.found(
                waitingCheckpoint("session-waiting", audience, wait), List.of())));
        recovery.dispatchRecoveredExternalWaits();

        assertEquals(1, events.size());
        DialogueExternalWaitDispatchEvent event = events.get(0);
        assertEquals(wait, event.externalWait());
        assertEquals(DialogueSessionStatus.WAITING, event.session().status());
        assertEquals(new DialogueSessionId("session-waiting"), event.session().sessionId());
    }

    private static DialogueCheckpoint activeCheckpoint(String sessionId, AudienceId audience) {
        CompiledDialogue dialogue = dialogue();
        DialogueSessionState state = new DialogueSessionEngine()
                .start(dialogue, new DialogueSessionId(sessionId), audience)
                .state();
        return DialogueCheckpoint.active(state, dialogue, CompiledDialogueFingerprinter.fingerprint(dialogue), 1000);
    }

    private static DialogueCheckpoint waitingCheckpoint(
            String sessionId,
            AudienceId audience,
            DialogueExternalWait wait
    ) {
        CompiledDialogue dialogue = dialogue();
        DialogueSessionState state = new DialogueSessionEngine()
                .start(dialogue, new DialogueSessionId(sessionId), audience)
                .state()
                .waiting(wait);
        return DialogueCheckpoint.active(state, dialogue, CompiledDialogueFingerprinter.fingerprint(dialogue), 1000);
    }

    private static DialogueCheckpoint completedCheckpoint(String sessionId, AudienceId audience) {
        CompiledDialogue dialogue = dialogue();
        DialogueSessionEngine engine = new DialogueSessionEngine();
        DialogueSessionState first = engine.start(dialogue, new DialogueSessionId(sessionId), audience).state();
        DialogueSessionState second = engine.apply(dialogue, first,
                DialogueInput.advance(new DialogueSessionId(sessionId), 0)).state();
        DialogueSessionState completed = engine.apply(dialogue, second,
                DialogueInput.advance(new DialogueSessionId(sessionId), 1)).state();
        return DialogueCheckpoint.terminal(completed, dialogue, CompiledDialogueFingerprinter.fingerprint(dialogue),
                1000, DialogueCheckpoint.TerminalFields.completed(DialogueOutcomeId.DEFAULT));
    }

    private static CompiledDialogue dialogue() {
        DialogueNodeId nodeId = new DialogueNodeId("greeting");
        return CompiledDialogue.linear(DialogueId.parse("sample:greeting"), nodeId, List.of(
                new CompiledDialogueLine(nodeId, 0, SpeakerId.parse("sample:father"), "Отец", "Где ты был?"),
                new CompiledDialogueLine(nodeId, 1, SpeakerId.parse("sample:player"), "Игрок", "Гулял.")
        ));
    }

    private static DialogueExternalWait externalWait() {
        return new DialogueExternalWait("echo_navigation:npc_move", "npc-move-1", "actor=jdh:john_father",
                new DialogueNodeId("greeting"), 0);
    }

}
