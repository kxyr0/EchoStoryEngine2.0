package io.github.kxyr0.echodialogue.forge.host;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.failure.DialogueFailureRequest;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.observation.DialogueTerminalEvent;
import io.github.kxyr0.echodialogue.api.service.DialogueOperationStatus;
import io.github.kxyr0.echodialogue.api.service.DialogueStartRequest;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionStatus;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogCandidate;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogState;
import io.github.kxyr0.echodialogue.forge.diagnostic.ForgeDiagnosticSink;
import io.github.kxyr0.echodialogue.forge.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echodialogue.forge.persistence.ForgePersistenceScheduler;
import io.github.kxyr0.echodialogue.persistence.file.FileDialogueSessionRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ForgeDialogueFailureServiceTest {
    @TempDir Path directory;
    @Test void failureIsCommittedBeforeNotificationAndRemainsFailedAfterRestart() throws Exception {
        var request = new DialogueStartRequest(new DialogueSessionId("failure-1"), DialogueId.parse("jdh:father"),
                new AudienceId(UUID.randomUUID().toString()));
        var terminal = new AtomicReference<DialogueTerminalEvent>();
        var host = host();
        try {
            await(host, () -> host.service().query(request.sessionId()).status() == DialogueOperationStatus.NOT_FOUND);
            assertEquals(DialogueOperationStatus.ACCEPTED, host.service().start(request).status());
            assertFalse(host.service().isDurablyAccepted(request.sessionId()));
            await(host, () -> host.service().isDurablyAccepted(request.sessionId()));
            assertEquals(0, host.service().query(request.sessionId()).session().orElseThrow().repositoryRevision());
            host.service().observeTerminal(request.sessionId(), terminal::set);
            assertEquals(DialogueOperationStatus.REJECTED,
                    host.service().fail(new DialogueFailureRequest(request.sessionId(), 99, "PROVIDER_LOST")).status());
            assertEquals(DialogueOperationStatus.ACCEPTED,
                    host.service().fail(new DialogueFailureRequest(request.sessionId(), 0, "PROVIDER_LOST")).status());
            assertNull(terminal.get());
            await(host, () -> terminal.get() != null);
            assertEquals(DialogueSessionStatus.FAILED, terminal.get().session().status());
            assertTrue(terminal.get().session().terminalOutcomeId().isEmpty());
        } finally { host.shutdown(); }
        var checkpoint = new FileDialogueSessionRepository(directory).load(request.sessionId())
                .checkpoint().orElseThrow();
        assertEquals(DialogueSessionPhase.FAILED, checkpoint.phase());
        assertEquals("FAILED", checkpoint.metadata().get("domainEvent.phase"));
        var restarted = host();
        try {
            await(restarted, () -> restarted.service().query(request.sessionId()).status()
                    == DialogueOperationStatus.ALREADY_TERMINAL);
            assertEquals(DialogueSessionStatus.FAILED,
                    restarted.service().query(request.sessionId()).session().orElseThrow().status());
        } finally { restarted.shutdown(); }
    }
    private ForgeDialogueHost host() {
        var node = new DialogueNodeId("start");
        var dialogue = CompiledDialogue.linear(DialogueId.parse("jdh:father"), node, List.of(
                new CompiledDialogueLine(node, 0, SpeakerId.parse("jdh:father"), "Father", "Line")));
        var catalog = new DialogueCatalogState();
        catalog.install(new DialogueCatalogCandidate(List.of(dialogue), List.of()));
        return new ForgeDialogueHost(catalog, new ForgeDiagnosticSink(new InMemoryDiagnosticBuffer(32)),
                new FileDialogueSessionRepository(directory), new ForgePersistenceScheduler("Failure Test"));
    }
    private void await(ForgeDialogueHost host, BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < deadline) {
            host.drainPersistenceCompletions();
            if (condition.getAsBoolean()) { return; }
            Thread.sleep(10);
        }
        assertTrue(condition.getAsBoolean());
    }
}
