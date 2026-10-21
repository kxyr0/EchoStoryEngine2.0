package io.github.kxyr0.echostoryengine.story.persistence.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionLoadResult;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionLoadStatus;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionSaveStatus;
import io.github.kxyr0.echostoryengine.story.persistence.PersistenceTestFixture;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class FileExecutionStateRepositorySuccessTest {
    @TempDir
    private Path root;

    @Test
    void saveAndLoadWaitingCheckpoint() {
        FileExecutionStateRepository repository = repository();

        assertEquals(ExecutionSaveStatus.SAVED, repository
                .save(PersistenceTestFixture.waitingCheckpoint(), -1)
                .status());
        ExecutionLoadResult loaded = repository.load(PersistenceTestFixture.EXECUTION_ID);

        assertEquals(ExecutionLoadStatus.FOUND, loaded.status());
        assertEquals(StoryExecutionPhase.WAITING, loaded.checkpoint().orElseThrow().state().phase());
        assertEquals(PersistenceTestFixture.ACTION_ID.asString(), loaded
                .checkpoint()
                .orElseThrow()
                .state()
                .pendingActionExecutionId());
    }

    @Test
    void notFoundReturnsExplicitResult() {
        ExecutionLoadResult loaded = repository().load(PersistenceTestFixture.EXECUTION_ID);

        assertEquals(ExecutionLoadStatus.NOT_FOUND, loaded.status());
        assertEquals("ESE-PERSISTENCE-003", loaded.diagnostics().get(0).code());
    }

    @Test
    void newSaveRequiresAbsentPrimary() {
        FileExecutionStateRepository repository = repository();

        repository.save(PersistenceTestFixture.waitingCheckpoint(), -1);

        assertEquals(ExecutionSaveStatus.REVISION_CONFLICT, repository
                .save(PersistenceTestFixture.waitingCheckpoint(), -1)
                .status());
    }

    @Test
    void updateRequiresMatchingRevision() {
        FileExecutionStateRepository repository = repository();

        repository.save(PersistenceTestFixture.waitingCheckpoint(), -1);

        assertEquals(ExecutionSaveStatus.REVISION_CONFLICT, repository
                .save(PersistenceTestFixture.finishedCheckpoint(), 2)
                .status());
        assertEquals(ExecutionSaveStatus.SAVED, repository
                .save(PersistenceTestFixture.finishedCheckpoint(), 3)
                .status());
    }

    @Test
    void updateCreatesOneBackup() throws IOException {
        FileExecutionStateRepository repository = repository();

        repository.save(PersistenceTestFixture.waitingCheckpoint(), -1);
        repository.save(PersistenceTestFixture.finishedCheckpoint(), 3);

        assertTrue(Files.exists(PersistenceTestFixture.backupFile(root)));
        assertTrue(Files.readString(PersistenceTestFixture.backupFile(root)).contains("\"phase\":\"WAITING\""));
    }

    @Test
    void storageFilenameDoesNotContainRawExecutionId() throws IOException {
        repository().save(PersistenceTestFixture.waitingCheckpoint(), -1);

        String filename = PersistenceTestFixture.primaryFile(root).getFileName().toString();

        assertFalse(filename.contains("execution"));
        assertTrue(filename.endsWith(".json"));
    }

    @Test
    void staleTempFileIsIgnoredOnLoad() throws IOException {
        FileExecutionStateRepository repository = repository();
        repository.save(PersistenceTestFixture.waitingCheckpoint(), -1);
        Path temp = root.resolve(PersistenceTestFixture.primaryFile(root).getFileName()
                .toString()
                .replace(".json", ".tmp"));

        Files.writeString(temp, "{broken");

        assertEquals(ExecutionLoadStatus.FOUND, repository.load(PersistenceTestFixture.EXECUTION_ID).status());
    }

    @Test
    void createdAtIsPreservedAndUpdatedAtChanges() throws IOException {
        PersistenceTestFixture.MutableClock clock = new PersistenceTestFixture.MutableClock();
        FileExecutionStateRepository repository = new FileExecutionStateRepository(root, clock);

        repository.save(PersistenceTestFixture.waitingCheckpoint(), -1);
        clock.set(PersistenceTestFixture.UPDATED);
        repository.save(PersistenceTestFixture.finishedCheckpoint(), 3);

        String content = Files.readString(PersistenceTestFixture.primaryFile(root));
        assertTrue(content.contains("\"createdAt\":\"2026-07-12T00:00:00Z\""));
        assertTrue(content.contains("\"updatedAt\":\"2026-07-12T00:01:00Z\""));
    }

    @Test
    void terminalCheckpointRoundTripsWithoutCommandData() {
        FileExecutionStateRepository repository = repository();

        repository.save(PersistenceTestFixture.finishedCheckpoint(), -1);
        ExecutionLoadResult loaded = repository.load(PersistenceTestFixture.EXECUTION_ID);

        assertEquals(StoryExecutionPhase.FINISHED, loaded.checkpoint().orElseThrow().state().phase());
        assertTrue(loaded.checkpoint().orElseThrow().pendingAction().isEmpty());
    }

    private FileExecutionStateRepository repository() {
        return new FileExecutionStateRepository(root, new PersistenceTestFixture.MutableClock());
    }
}
