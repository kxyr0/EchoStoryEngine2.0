package io.github.kxyr0.echodialogue.persistence.file;

import static io.github.kxyr0.echodialogue.persistence.PersistenceTestFixtures.SESSION_ID;
import static io.github.kxyr0.echodialogue.persistence.PersistenceTestFixtures.activeCheckpoint;
import static io.github.kxyr0.echodialogue.persistence.PersistenceTestFixtures.completedCheckpoint;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryLoadResult;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryWriteResult;
import io.github.kxyr0.echodialogue.core.persistence.DialogueSessionRepository.SaveExpectation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileDialogueSessionRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void newSnapshotSaveAndFreshRepositoryLoad() {
        FileDialogueSessionRepository repository = new FileDialogueSessionRepository(tempDir);

        DialogueRepositoryWriteResult saved = repository.save(activeCheckpoint(1000), SaveExpectation.expectedEmpty());
        DialogueRepositoryLoadResult loaded = new FileDialogueSessionRepository(tempDir).load(SESSION_ID);

        assertEquals(DialogueRepositoryWriteResult.Kind.CREATED, saved.kind());
        assertEquals(DialogueRepositoryLoadResult.Kind.FOUND, loaded.kind());
        assertEquals(SESSION_ID, loaded.checkpoint().orElseThrow().sessionId());
    }

    @Test
    void staleRepositoryRevisionDoesNotOverwritePrimary() {
        FileDialogueSessionRepository repository = new FileDialogueSessionRepository(tempDir);
        DialogueCheckpoint created = repository.save(activeCheckpoint(1000), SaveExpectation.expectedEmpty())
                .checkpoint()
                .orElseThrow();

        DialogueRepositoryWriteResult updated = repository.save(
                activeCheckpoint(2000), SaveExpectation.expectedRevision(created.repositoryRevision()));
        DialogueRepositoryWriteResult conflict = repository.save(
                activeCheckpoint(3000), SaveExpectation.expectedRevision(created.repositoryRevision()));

        assertEquals(DialogueRepositoryWriteResult.Kind.UPDATED, updated.kind());
        assertEquals(DialogueRepositoryWriteResult.Kind.REVISION_CONFLICT, conflict.kind());
    }

    @Test
    void corruptPrimaryFallsBackToOneBackup() throws IOException {
        FileDialogueSessionRepository repository = new FileDialogueSessionRepository(tempDir);
        DialogueCheckpoint created = repository.save(activeCheckpoint(1000), SaveExpectation.expectedEmpty())
                .checkpoint()
                .orElseThrow();
        repository.save(activeCheckpoint(2000), SaveExpectation.expectedRevision(created.repositoryRevision()));
        Files.writeString(primaryFile(), "{", StandardCharsets.UTF_8);

        DialogueRepositoryLoadResult loaded = repository.load(SESSION_ID);

        assertEquals(DialogueRepositoryLoadResult.Kind.FOUND, loaded.kind());
        assertTrue(loaded.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("EDLG-PERSIST-005")));
    }

    @Test
    void loadAllFindsBackupOnlySnapshotAfterInterruptedReplacement() throws IOException {
        FileDialogueSessionRepository repository = new FileDialogueSessionRepository(tempDir);
        repository.save(activeCheckpoint(1000), SaveExpectation.expectedEmpty());
        Files.move(primaryFile(), backupFile());

        List<DialogueRepositoryLoadResult> loaded = repository.loadAll();

        assertEquals(1, loaded.size());
        assertEquals(DialogueRepositoryLoadResult.Kind.FOUND, loaded.get(0).kind());
        assertEquals(SESSION_ID, loaded.get(0).checkpoint().orElseThrow().sessionId());
    }

    @Test
    void corruptPrimaryAndBackupFailExplicitly() throws IOException {
        FileDialogueSessionRepository repository = new FileDialogueSessionRepository(tempDir);
        DialogueCheckpoint created = repository.save(activeCheckpoint(1000), SaveExpectation.expectedEmpty())
                .checkpoint()
                .orElseThrow();
        repository.save(activeCheckpoint(2000), SaveExpectation.expectedRevision(created.repositoryRevision()));
        Files.writeString(primaryFile(), "{", StandardCharsets.UTF_8);
        Files.writeString(backupFile(), "{", StandardCharsets.UTF_8);

        DialogueRepositoryLoadResult loaded = repository.load(SESSION_ID);

        assertEquals(DialogueRepositoryLoadResult.Kind.FAILED, loaded.kind());
        assertTrue(loaded.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("EDLG-PERSIST-006")));
    }

    @Test
    void terminalAcknowledgementAndCleanupNeverRemoveActive() {
        FileDialogueSessionRepository repository = new FileDialogueSessionRepository(tempDir);
        DialogueCheckpoint active = repository.save(activeCheckpoint(1000), SaveExpectation.expectedEmpty())
                .checkpoint()
                .orElseThrow();
        DialogueCheckpoint terminal = completedCheckpoint(1000);
        repository.save(terminal, SaveExpectation.expectedEmpty());

        DialogueRepositoryWriteResult cleanup = repository.cleanupTerminal(10_000, 1, 10);
        DialogueRepositoryWriteResult activeAck = repository.acknowledgeTerminal(
                active.sessionId(), active.repositoryRevision());

        assertEquals(1, cleanup.affectedRecords());
        assertEquals(DialogueRepositoryWriteResult.Kind.FAILED, activeAck.kind());
        assertEquals(DialogueRepositoryLoadResult.Kind.FOUND, repository.load(active.sessionId()).kind());
    }

    private Path primaryFile() throws IOException {
        try (var stream = Files.list(tempDir)) {
            return stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .findFirst()
                    .orElseThrow();
        }
    }

    private Path backupFile() throws IOException {
        Path primary = primaryFile();
        String name = primary.getFileName().toString().replaceFirst("\\.json$", ".bak");
        return primary.resolveSibling(name);
    }
}
