package io.github.kxyr0.echodialogue.forge.host;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.outcome.DialogueOutcomeId;
import io.github.kxyr0.echodialogue.api.service.DialogueOperationStatus;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.service.DialogueStartRequest;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionStatus;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalResumeRequest;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWaitRequest;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueCompletion;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueNode;
import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;
import io.github.kxyr0.echodialogue.core.session.DialogueInput;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import io.github.kxyr0.echodialogue.core.session.DialogueTransition;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogCandidate;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogState;
import io.github.kxyr0.echodialogue.forge.diagnostic.ForgeDiagnosticSink;
import io.github.kxyr0.echodialogue.forge.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echodialogue.forge.persistence.ForgePersistenceScheduler;
import io.github.kxyr0.echodialogue.persistence.file.FileDialogueSessionRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ForgeDialoguePublicExternalServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void publicExternalWaitAndResumeAreDurableAndOpaque() throws InterruptedException {
        ForgeDialogueHost host = host();
        DialogueService service = host.service();
        DialogueStartRequest request = request("session-4");
        DialogueExternalWait wait = new DialogueExternalWait(
                "echo_trigger", "trigger-instance-1", "PlayerEnter(Exit)",
                new DialogueNodeId("greeting"), 0);
        try {
            awaitRecovery(host, service);
            service.start(request);
            awaitActiveInstalled(host);

            assertEquals(DialogueOperationStatus.ACCEPTED, service.waitExternal(
                    new DialogueExternalWaitRequest(request.sessionId(), 0, wait)).status());
            awaitQueryStatus(host, service, request.sessionId(), DialogueOperationStatus.WAITING);
            assertEquals(wait, service.query(request.sessionId()).session().orElseThrow()
                    .externalWait().orElseThrow());
            awaitRuntimePhase(host, request.audienceId(), DialogueSessionPhase.WAITING);

            assertEquals(DialogueOperationStatus.ACCEPTED, service.resumeExternal(
                    new DialogueExternalResumeRequest(request.sessionId(), 1, wait.waitKey())).status());
            awaitQueryStatus(host, service, request.sessionId(), DialogueOperationStatus.ALREADY_ACTIVE);
            awaitRuntimePhase(host, request.audienceId(), DialogueSessionPhase.ACTIVE);
            assertTrue(service.query(request.sessionId()).session().orElseThrow().externalWait().isEmpty());
        } finally {
            host.shutdown();
        }
    }

    @Test
    void publicExternalResumeAcceptsTerminalCommandCompletion() throws InterruptedException {
        ForgeDialogueHost host = host(terminalCommandDialogue());
        DialogueService service = host.service();
        DialogueStartRequest request = request("session-5", "sample:terminal_command");
        try {
            awaitRecovery(host, service);
            service.start(request);
            awaitActiveInstalled(host);

            advance(host, request, 0);
            awaitQueryStatus(host, service, request.sessionId(), DialogueOperationStatus.WAITING);
            DialogueExternalWait wait = service.query(request.sessionId()).session().orElseThrow()
                    .externalWait().orElseThrow();

            assertEquals(DialogueOperationStatus.ACCEPTED, service.resumeExternal(
                    new DialogueExternalResumeRequest(request.sessionId(), 1, wait.waitKey())).status());

            awaitQueryStatus(host, service, request.sessionId(), DialogueOperationStatus.ALREADY_TERMINAL);
            assertEquals(DialogueSessionStatus.COMPLETED,
                    service.query(request.sessionId()).session().orElseThrow().status());
            assertEquals(DialogueOutcomeId.parse("sample:done"), service.query(request.sessionId())
                    .session().orElseThrow().terminalOutcomeId().orElseThrow());
        } finally {
            host.shutdown();
        }
    }

    private ForgeDialogueHost host() {
        return host(dialogue());
    }

    private ForgeDialogueHost host(CompiledDialogue... dialogues) {
        DialogueCatalogState catalog = new DialogueCatalogState();
        catalog.install(new DialogueCatalogCandidate(List.of(dialogues), List.of()));
        return new ForgeDialogueHost(catalog, new ForgeDiagnosticSink(new InMemoryDiagnosticBuffer(32)),
                new FileDialogueSessionRepository(tempDir), new ForgePersistenceScheduler("Public External Test"));
    }

    private void advance(ForgeDialogueHost host, DialogueStartRequest request, int revision) {
        DialogueTransition transition = host.sessions.preview(request.audienceId(),
                        DialogueInput.advance(request.sessionId(), revision))
                .orElseThrow();
        host.saveFlow.saveOrDeliver(Optional.empty(), request.audienceId(), transition);
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

    private void awaitRuntimePhase(
            ForgeDialogueHost host,
            AudienceId audienceId,
            DialogueSessionPhase expected
    ) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 1_000;
        while (System.currentTimeMillis() < deadline) {
            host.drainPersistenceCompletions();
            if (host.sessions.stateFor(audienceId).map(state -> state.phase() == expected).orElse(false)) {
                return;
            }
            Thread.sleep(10);
        }
        host.drainPersistenceCompletions();
        assertEquals(expected, host.sessions.stateFor(audienceId).orElseThrow().phase());
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

    private static CompiledDialogue terminalCommandDialogue() {
        DialogueNodeId nodeId = new DialogueNodeId("greeting");
        return new CompiledDialogue(DialogueId.parse("sample:terminal_command"), nodeId, List.of(
                new CompiledDialogueNode(nodeId, List.of(
                        new CompiledDialogueLine(nodeId, 0, SpeakerId.parse("sample:father"),
                                "Father", "Last line.")), List.of(),
                        List.of(new CompiledDialogueCommand("test:terminal", 1, Map.of("order", "last"))),
                        Optional.of(new CompiledDialogueCompletion(DialogueOutcomeId.parse("sample:done"))),
                        Optional.empty())));
    }

    @FunctionalInterface
    private interface StatusProbe {
        DialogueOperationStatus status();
    }
}
