package io.github.kxyr0.echostoryengine.story.persistence.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionLoadResult;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionLoadStatus;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionSaveStatus;
import io.github.kxyr0.echostoryengine.story.persistence.PersistenceTestFixture;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class FileExecutionStateRepositoryFailureTest {
    @TempDir
    private Path root;

    @Test
    void malformedPrimaryWithoutBackupFailsLoad() throws IOException {
        FileExecutionStateRepository repository = repository();
        repository.save(PersistenceTestFixture.waitingCheckpoint(), -1);
        Files.writeString(PersistenceTestFixture.primaryFile(root), "{broken");

        ExecutionLoadResult loaded = repository.load(PersistenceTestFixture.EXECUTION_ID);

        assertEquals(ExecutionLoadStatus.FAILED, loaded.status());
        assertCodes(loaded, "ESE-PERSISTENCE-004", "ESE-PERSISTENCE-012");
    }

    @Test
    void unsupportedSchemaIsRejected() throws IOException {
        FileExecutionStateRepository repository = repository();
        repository.save(PersistenceTestFixture.waitingCheckpoint(), -1);
        replacePrimary("\"saveSchemaVersion\":1", "\"saveSchemaVersion\":2");

        ExecutionLoadResult loaded = repository.load(PersistenceTestFixture.EXECUTION_ID);

        assertEquals(ExecutionLoadStatus.FAILED, loaded.status());
        assertCodes(loaded, "ESE-PERSISTENCE-005", "ESE-PERSISTENCE-012");
    }

    @Test
    void unknownRootFieldIsRejected() throws IOException {
        FileExecutionStateRepository repository = repository();
        repository.save(PersistenceTestFixture.waitingCheckpoint(), -1);
        replacePrimary("\"updatedAt\"", "\"unknown\":\"x\",\"updatedAt\"");

        assertEquals(ExecutionLoadStatus.FAILED, repository.load(PersistenceTestFixture.EXECUTION_ID).status());
    }

    @Test
    void missingRootFieldIsRejected() throws IOException {
        FileExecutionStateRepository repository = repository();
        repository.save(PersistenceTestFixture.waitingCheckpoint(), -1);
        replacePrimary("\"failureCode\":\"\",", "");

        assertEquals(ExecutionLoadStatus.FAILED, repository.load(PersistenceTestFixture.EXECUTION_ID).status());
    }

    @Test
    void invalidExecutionIdIsRejected() throws IOException {
        FileExecutionStateRepository repository = repository();
        repository.save(PersistenceTestFixture.waitingCheckpoint(), -1);
        replacePrimary("test:execution", "invalid");

        assertEquals(ExecutionLoadStatus.FAILED, repository.load(PersistenceTestFixture.EXECUTION_ID).status());
    }

    @Test
    void invalidPhaseIsRejected() throws IOException {
        FileExecutionStateRepository repository = repository();
        repository.save(PersistenceTestFixture.waitingCheckpoint(), -1);
        replacePrimary("\"phase\":\"WAITING\"", "\"phase\":\"UNKNOWN\"");

        assertEquals(ExecutionLoadStatus.FAILED, repository.load(PersistenceTestFixture.EXECUTION_ID).status());
    }

    @Test
    void decimalRevisionIsRejected() throws IOException {
        FileExecutionStateRepository repository = repository();
        repository.save(PersistenceTestFixture.waitingCheckpoint(), -1);
        replacePrimary("\"executionRevision\":3", "\"executionRevision\":3.5");

        assertEquals(ExecutionLoadStatus.FAILED, repository.load(PersistenceTestFixture.EXECUTION_ID).status());
    }

    @Test
    void invalidPendingActionIdIsRejected() throws IOException {
        FileExecutionStateRepository repository = repository();
        repository.save(PersistenceTestFixture.waitingCheckpoint(), -1);
        replacePrimary(PersistenceTestFixture.ACTION_ID.asString(), "bad");

        assertEquals(ExecutionLoadStatus.FAILED, repository.load(PersistenceTestFixture.EXECUTION_ID).status());
    }

    @Test
    void corruptPrimaryFallsBackToBackup() throws IOException {
        FileExecutionStateRepository repository = repository();
        repository.save(PersistenceTestFixture.waitingCheckpoint(), -1);
        repository.save(PersistenceTestFixture.finishedCheckpoint(), 3);
        Files.writeString(PersistenceTestFixture.primaryFile(root), "{broken");

        ExecutionLoadResult loaded = repository.load(PersistenceTestFixture.EXECUTION_ID);

        assertEquals(ExecutionLoadStatus.FOUND, loaded.status());
        assertEquals("ESE-PERSISTENCE-011", loaded.diagnostics().get(0).code());
    }

    @Test
    void corruptPrimaryAndBackupFailLoad() throws IOException {
        FileExecutionStateRepository repository = repository();
        repository.save(PersistenceTestFixture.waitingCheckpoint(), -1);
        repository.save(PersistenceTestFixture.finishedCheckpoint(), 3);
        Files.writeString(PersistenceTestFixture.primaryFile(root), "{broken");
        Files.writeString(PersistenceTestFixture.backupFile(root), "{broken");

        assertEquals(ExecutionLoadStatus.FAILED, repository.load(PersistenceTestFixture.EXECUTION_ID).status());
    }

    @Test
    void saveRefusesCorruptExistingPrimary() throws IOException {
        FileExecutionStateRepository repository = repository();
        repository.save(PersistenceTestFixture.waitingCheckpoint(), -1);
        Files.writeString(PersistenceTestFixture.primaryFile(root), "{broken");

        assertEquals(ExecutionSaveStatus.FAILED, repository
                .save(PersistenceTestFixture.finishedCheckpoint(), 3)
                .status());
    }

    private void replacePrimary(String target, String replacement) throws IOException {
        Path primary = PersistenceTestFixture.primaryFile(root);
        Files.writeString(primary, Files.readString(primary).replace(target, replacement));
    }

    private void assertCodes(ExecutionLoadResult loaded, String first, String second) {
        assertTrue(loaded.diagnostics().stream().anyMatch(diagnostic -> first.equals(diagnostic.code())));
        assertTrue(loaded.diagnostics().stream().anyMatch(diagnostic -> second.equals(diagnostic.code())));
    }

    private FileExecutionStateRepository repository() {
        return new FileExecutionStateRepository(root, new PersistenceTestFixture.MutableClock());
    }
}
