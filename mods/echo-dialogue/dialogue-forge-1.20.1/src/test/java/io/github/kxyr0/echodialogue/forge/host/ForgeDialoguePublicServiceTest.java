package io.github.kxyr0.echodialogue.forge.host;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.observation.DialogueTerminalEvent;
import io.github.kxyr0.echodialogue.api.outcome.DialogueOutcomeId;
import io.github.kxyr0.echodialogue.api.service.DialogueOperationStatus;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.service.DialogueStartRequest;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionStatus;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import io.github.kxyr0.echodialogue.core.session.DialogueInput;
import io.github.kxyr0.echodialogue.core.session.DialogueTransition;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogCandidate;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogState;
import io.github.kxyr0.echodialogue.forge.diagnostic.ForgeDiagnosticSink;
import io.github.kxyr0.echodialogue.forge.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echodialogue.forge.persistence.ForgePersistenceScheduler;
import io.github.kxyr0.echodialogue.persistence.file.FileDialogueSessionRepository;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ForgeDialoguePublicServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void publicStartIsRecoveryGatedAndIdempotent() throws InterruptedException {
        ForgeDialogueHost host = host();
        DialogueService service = host.service();
        DialogueStartRequest request = request("session-1");
        try {
            assertEquals(DialogueOperationStatus.SERVICE_UNAVAILABLE, service.start(request).status());
            awaitRecovery(host, service);

            assertEquals(DialogueOperationStatus.ACCEPTED, service.start(request).status());
            assertEquals(DialogueOperationStatus.ALREADY_ACTIVE, service.start(request).status());
            awaitActiveInstalled(host);

            assertEquals(DialogueSessionStatus.ACTIVE,
                    service.query(request.sessionId()).session().orElseThrow().status());
        } finally {
            host.shutdown();
        }
    }

    @Test
    void publicCompletionRetainsOutcomeAndNotifiesObserverAfterSave() throws InterruptedException {
        ForgeDialogueHost host = host();
        DialogueService service = host.service();
        DialogueStartRequest request = request("session-3");
        List<DialogueTerminalEvent> terminalEvents = new ArrayList<>();
        try {
            awaitRecovery(host, service);
            service.start(request);
            awaitActiveInstalled(host);
            service.observeTerminal(request.sessionId(), terminalEvents::add);

            advance(host, request, 0);
            awaitQueryRevision(host, service, request.sessionId(), 1);
            advance(host, request, 1);
            awaitQueryStatus(host, service, request.sessionId(), DialogueOperationStatus.ALREADY_TERMINAL);

            assertEquals(1, terminalEvents.size());
            assertEquals(DialogueSessionStatus.COMPLETED, terminalEvents.get(0).session().status());
            assertEquals(DialogueOutcomeId.DEFAULT, terminalEvents.get(0).session().terminalOutcomeId().orElseThrow());
            assertEquals(DialogueOutcomeId.DEFAULT, service.query(request.sessionId())
                    .session().orElseThrow().terminalOutcomeId().orElseThrow());
        } finally {
            host.shutdown();
        }
    }

    @Test
    void publicCancelRetainsTerminalAndNotifiesObserverAfterSave() throws InterruptedException {
        ForgeDialogueHost host = host();
        DialogueService service = host.service();
        DialogueStartRequest request = request("session-2");
        List<DialogueTerminalEvent> terminalEvents = new ArrayList<>();
        try {
            awaitRecovery(host, service);
            service.start(request);
            awaitActiveInstalled(host);

            service.observeTerminal(request.sessionId(), terminalEvents::add);
            assertEquals(DialogueOperationStatus.ACCEPTED, service.cancel(request.sessionId()).status());
            awaitQueryStatus(host, service, request.sessionId(), DialogueOperationStatus.ALREADY_TERMINAL);

            assertEquals(1, terminalEvents.size());
            assertEquals(DialogueSessionStatus.CANCELLED, terminalEvents.get(0).session().status());
            assertEquals("explicit-cancel", terminalEvents.get(0).session().cancellationReason().orElseThrow());
        } finally {
            host.shutdown();
        }
    }

    private void advance(ForgeDialogueHost host, DialogueStartRequest request, int revision) {
        DialogueTransition transition = host.sessions.preview(request.audienceId(),
                        DialogueInput.advance(request.sessionId(), revision))
                .orElseThrow();
        host.saveFlow.saveOrDeliver(Optional.empty(), request.audienceId(), transition);
    }

    private ForgeDialogueHost host() {
        return host(dialogue());
    }

    private ForgeDialogueHost host(CompiledDialogue... dialogues) {
        DialogueCatalogState catalog = new DialogueCatalogState();
        catalog.install(new DialogueCatalogCandidate(List.of(dialogues), List.of()));
        return new ForgeDialogueHost(catalog, new ForgeDiagnosticSink(new InMemoryDiagnosticBuffer(32)),
                new FileDialogueSessionRepository(tempDir), new ForgePersistenceScheduler("Public Service Test"));
    }

    private void awaitRecovery(ForgeDialogueHost host, DialogueService service) throws InterruptedException {
        awaitStatus(host, () -> service.query(new DialogueSessionId("probe")).status(),
                DialogueOperationStatus.NOT_FOUND);
    }

    private void awaitQueryStatus(
            ForgeDialogueHost host,
            DialogueService service,
            DialogueSessionId sessionId,
            DialogueOperationStatus expected
    ) throws InterruptedException {
        awaitStatus(host, () -> service.query(sessionId).status(), expected);
    }

    private void awaitQueryRevision(
            ForgeDialogueHost host,
            DialogueService service,
            DialogueSessionId sessionId,
            int expected
    ) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 1_000;
        while (System.currentTimeMillis() < deadline) {
            host.drainPersistenceCompletions();
            if (service.query(sessionId).session().map(view -> view.revision() == expected).orElse(false)) {
                return;
            }
            Thread.sleep(10);
        }
        host.drainPersistenceCompletions();
        assertEquals(expected, service.query(sessionId).session().orElseThrow().revision());
    }

    private void awaitActiveInstalled(ForgeDialogueHost host) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 1_000;
        while (System.currentTimeMillis() < deadline) {
            host.drainPersistenceCompletions();
            if (host.activeCount() == 1) {
                return;
            }
            Thread.sleep(10);
        }
        host.drainPersistenceCompletions();
        assertEquals(1, host.activeCount());
    }

    private void awaitStatus(
            ForgeDialogueHost host,
            StatusProbe probe,
            DialogueOperationStatus expected
    ) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 1_000;
        while (System.currentTimeMillis() < deadline) {
            host.drainPersistenceCompletions();
            if (probe.status() == expected) {
                return;
            }
            Thread.sleep(10);
        }
        host.drainPersistenceCompletions();
        assertEquals(expected, probe.status());
    }

    private DialogueStartRequest request(String sessionId) {
        return request(sessionId, "sample:greeting");
    }

    private DialogueStartRequest request(String sessionId, String dialogueId) {
        return new DialogueStartRequest(new DialogueSessionId(sessionId),
                DialogueId.parse(dialogueId), new AudienceId("audience-1"));
    }

    private static CompiledDialogue dialogue() {
        DialogueNodeId nodeId = new DialogueNodeId("greeting");
        return CompiledDialogue.linear(DialogueId.parse("sample:greeting"), nodeId, List.of(
                new CompiledDialogueLine(nodeId, 0, SpeakerId.parse("sample:father"), "Father", "Where were you?")));
    }

    @FunctionalInterface
    private interface StatusProbe {
        DialogueOperationStatus status();
    }
}
