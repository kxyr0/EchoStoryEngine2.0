package io.github.kxyr0.echodialogue.forge.host;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.service.DialogueOperationStatus;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.service.DialogueStartRequest;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueNode;
import io.github.kxyr0.echodialogue.core.session.DialogueInput;
import io.github.kxyr0.echodialogue.core.session.DialogueTransition;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogCandidate;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogState;
import io.github.kxyr0.echodialogue.forge.diagnostic.ForgeDiagnosticSink;
import io.github.kxyr0.echodialogue.forge.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echodialogue.forge.persistence.ForgePersistenceScheduler;
import io.github.kxyr0.echodialogue.persistence.file.FileDialogueSessionRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ForgeDialogueLoadServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void dialogueLoadStartsTargetDialogueAfterDurableSave() throws InterruptedException {
        ForgeDialogueHost host = host();
        DialogueService service = host.service();
        DialogueStartRequest request = request();
        try {
            awaitRecovery(host, service);
            service.start(request);
            awaitActiveInstalled(host);

            advance(host, request, 0);
            awaitQueryRevision(host, service, request.sessionId(), 1);

            assertEquals(DialogueId.parse("sample:street"), service.query(request.sessionId())
                    .session().orElseThrow().dialogueId());
            assertEquals(DialogueId.parse("sample:street"),
                    host.sessions.stateFor(request.audienceId()).orElseThrow().dialogueId());
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
        DialogueCatalogState catalog = new DialogueCatalogState();
        catalog.install(new DialogueCatalogCandidate(List.of(loadingDialogue(), loadedDialogue()), List.of()));
        return new ForgeDialogueHost(catalog, new ForgeDiagnosticSink(new InMemoryDiagnosticBuffer(32)),
                new FileDialogueSessionRepository(tempDir), new ForgePersistenceScheduler("Load Service Test"));
    }

    private void awaitRecovery(ForgeDialogueHost host, DialogueService service) throws InterruptedException {
        awaitStatus(host, () -> service.query(new DialogueSessionId("probe")).status(),
                DialogueOperationStatus.NOT_FOUND);
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
            if (host.activeCount() == 1) { return; }
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
            if (probe.status() == expected) { return; }
            Thread.sleep(10);
        }
        host.drainPersistenceCompletions();
        assertEquals(expected, probe.status());
    }

    private DialogueStartRequest request() {
        return new DialogueStartRequest(new DialogueSessionId("session-load"),
                DialogueId.parse("sample:home"), new AudienceId("audience-1"));
    }

    private static CompiledDialogue loadingDialogue() {
        DialogueId targetId = DialogueId.parse("sample:street");
        DialogueNodeId nodeId = new DialogueNodeId("home");
        CompiledDialogueNode node = new CompiledDialogueNode(nodeId,
                List.of(new CompiledDialogueLine(nodeId, 0, SpeakerId.parse("sample:father"), "Father", "Go.")),
                List.of(), Optional.empty(), Optional.of(CompiledDialogueNode.Transition.load(targetId)));
        return new CompiledDialogue(DialogueId.parse("sample:home"), nodeId, List.of(node));
    }

    private static CompiledDialogue loadedDialogue() {
        DialogueNodeId nodeId = new DialogueNodeId("street");
        return CompiledDialogue.linear(DialogueId.parse("sample:street"), nodeId, List.of(
                new CompiledDialogueLine(nodeId, 0, SpeakerId.parse("sample:player"), "Player", "Street.")));
    }

    @FunctionalInterface
    private interface StatusProbe {
        DialogueOperationStatus status();
    }
}
