package io.github.kxyr0.echodialogue.forge.host;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogState;
import io.github.kxyr0.echodialogue.forge.diagnostic.ForgeDiagnosticSink;
import io.github.kxyr0.echodialogue.forge.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echodialogue.forge.persistence.ForgePersistenceScheduler;
import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryLoadResult;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryRevision;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryWriteResult;
import io.github.kxyr0.echodialogue.core.persistence.DialogueSessionRepository;
import io.github.kxyr0.echodialogue.persistence.file.FileDialogueSessionRepository;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ForgeDialogueHostLifecycleTest {
    @TempDir
    Path tempDir;

    @Test
    void shutdownIsIdempotentAndEmitsStoppedDiagnosticOnce() {
        InMemoryDiagnosticBuffer buffer = new InMemoryDiagnosticBuffer(10);
        ForgeDialogueHost host = new ForgeDialogueHost(
                new DialogueCatalogState(),
                new ForgeDiagnosticSink(buffer),
                new FileDialogueSessionRepository(tempDir),
                new ForgePersistenceScheduler("Echo Dialogue Test Persistence"));

        host.shutdown();
        host.shutdown();

        assertTrue(host.stopped);
        assertEquals(0, host.activeCount());
        assertEquals(1, buffer.recent().stream()
                .filter(diagnostic -> diagnostic.code().equals("EDLG-FORGE-007"))
                .count());
    }

    @Test
    void recoveryLoadRuntimeFailureCompletesWithDiagnostic() throws InterruptedException {
        InMemoryDiagnosticBuffer buffer = new InMemoryDiagnosticBuffer(10);
        ForgeDialogueHost host = new ForgeDialogueHost(
                new DialogueCatalogState(),
                new ForgeDiagnosticSink(buffer),
                new ThrowingLoadRepository(),
                new ForgePersistenceScheduler("Echo Dialogue Throwing Recovery Test"));
        try {
            awaitDiagnostic(host, buffer, "EDLG-FORGE-018");

            assertEquals(1, host.status().corruptSnapshots());
            assertEquals(0, host.status().droppedPersistenceCompletions());
        } finally {
            host.shutdown();
        }
    }

    private static void awaitDiagnostic(
            ForgeDialogueHost host,
            InMemoryDiagnosticBuffer buffer,
            String code
    ) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 1_000;
        while (System.currentTimeMillis() < deadline) {
            host.drainPersistenceCompletions();
            if (hasDiagnostic(buffer, code)) {
                return;
            }
            Thread.sleep(10);
        }
        host.drainPersistenceCompletions();
        assertTrue(hasDiagnostic(buffer, code));
    }

    private static boolean hasDiagnostic(InMemoryDiagnosticBuffer buffer, String code) {
        return buffer.recent().stream().anyMatch(diagnostic -> diagnostic.code().equals(code));
    }

    private static final class ThrowingLoadRepository implements DialogueSessionRepository {
        @Override
        public DialogueRepositoryLoadResult load(DialogueSessionId sessionId) {
            return DialogueRepositoryLoadResult.notFound();
        }

        @Override
        public List<DialogueRepositoryLoadResult> loadAll() {
            throw new IllegalStateException("load failed");
        }

        @Override
        public DialogueRepositoryWriteResult save(DialogueCheckpoint checkpoint, SaveExpectation expectation) {
            return DialogueRepositoryWriteResult.failed(List.of());
        }

        @Override
        public DialogueRepositoryWriteResult acknowledgeTerminal(
                DialogueSessionId sessionId,
                DialogueRepositoryRevision expectedRevision
        ) {
            return DialogueRepositoryWriteResult.notFound();
        }

        @Override
        public DialogueRepositoryWriteResult cleanupTerminal(long nowEpochMillis, long maxAgeMillis, int maxRecords) {
            return DialogueRepositoryWriteResult.completed(0, List.of());
        }
    }
}
