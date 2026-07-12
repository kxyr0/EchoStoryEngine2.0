package io.github.kxyr0.echostoryengine.story.persistence.file;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionCheckpoint;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionLoadResult;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionSaveResult;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionStateRepository;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import io.github.kxyr0.echostoryengine.story.persistence.diagnostic.PersistenceDiagnosticCatalog;
import io.github.kxyr0.echostoryengine.story.persistence.schema.ExecutionSnapshot;
import io.github.kxyr0.echostoryengine.story.persistence.schema.ExecutionSnapshotCodec;
import io.github.kxyr0.echostoryengine.story.persistence.schema.ExecutionSnapshotTranslator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Synchronous durable file adapter for execution checkpoints.
 */
public final class FileExecutionStateRepository implements ExecutionStateRepository {
    private final Path rootDirectory;
    private final StoryClock clock;
    private final PersistenceDiagnosticCatalog diagnostics = new PersistenceDiagnosticCatalog();
    private final SnapshotFileReader reader;
    private final SnapshotFileWriter writer;
    private final ExecutionStorageKey storageKey = new ExecutionStorageKey();

    public FileExecutionStateRepository(Path rootDirectory, StoryClock clock) {
        this.rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory");
        this.clock = Objects.requireNonNull(clock, "clock");
        ExecutionSnapshotCodec codec = new ExecutionSnapshotCodec();
        ExecutionSnapshotTranslator translator = new ExecutionSnapshotTranslator();
        this.reader = new SnapshotFileReader(codec, translator);
        this.writer = new SnapshotFileWriter(codec, translator);
    }

    @Override
    public ExecutionLoadResult load(ExecutionId executionId) {
        ExecutionId checked = Objects.requireNonNull(executionId, "executionId");
        Instant occurredAt = clock.now();
        ExecutionFileSet files = storageKey.files(rootDirectory, checked);
        SnapshotRead primary = reader.read(files.primary());
        if (primary.status() == SnapshotReadStatus.MISSING) {
            return ExecutionLoadResult.notFound(List.of(diag("ESE-PERSISTENCE-003", checked, occurredAt)));
        }
        if (primary.checkpoint().isPresent()) {
            return ExecutionLoadResult.found(primary.checkpoint().orElseThrow(), List.of());
        }
        return loadBackup(checked, occurredAt, files, primary);
    }

    @Override
    public ExecutionSaveResult save(ExecutionCheckpoint checkpoint, int expectedRevision) {
        ExecutionCheckpoint checked = Objects.requireNonNull(checkpoint, "checkpoint");
        Instant occurredAt = clock.now();
        ExecutionFileSet files = storageKey.files(rootDirectory, checked.state().executionId());
        try {
            Files.createDirectories(rootDirectory);
            SaveReadResult current = currentSnapshot(files.primary(), checked, expectedRevision, occurredAt);
            if (current.rejected().isPresent()) {
                return current.rejected().orElseThrow();
            }
            writer.write(files, checked, current.snapshot(), occurredAt);
            return ExecutionSaveResult.saved(List.of());
        } catch (IOException | RuntimeException exception) {
            return ExecutionSaveResult.failed(List.of(saveFailed(checked, occurredAt, exception)));
        }
    }

    private ExecutionLoadResult loadBackup(
            ExecutionId executionId,
            Instant occurredAt,
            ExecutionFileSet files,
            SnapshotRead primary) {
        SnapshotRead backup = reader.read(files.backup());
        if (backup.checkpoint().isPresent()) {
            StoryDiagnostic diagnostic = diag("ESE-PERSISTENCE-011", executionId, occurredAt);
            return ExecutionLoadResult.found(backup.checkpoint().orElseThrow(), List.of(diagnostic));
        }
        String code = primary.diagnosticCode().isBlank() ? "ESE-PERSISTENCE-004" : primary.diagnosticCode();
        return ExecutionLoadResult.failed(List.of(
                diag(code, executionId, occurredAt),
                diag("ESE-PERSISTENCE-012", executionId, occurredAt)));
    }

    private SaveReadResult currentSnapshot(
            Path primary,
            ExecutionCheckpoint checkpoint,
            int expectedRevision,
            Instant occurredAt) {
        SnapshotRead current = reader.read(primary);
        if (current.status() == SnapshotReadStatus.MISSING) {
            return absentSnapshot(checkpoint, expectedRevision, occurredAt);
        }
        if (current.status() == SnapshotReadStatus.CORRUPT) {
            return malformedSnapshot(checkpoint, current.diagnosticCode(), occurredAt);
        }
        return checkedRevision(current.snapshot().orElseThrow(), checkpoint, expectedRevision, occurredAt);
    }

    private SaveReadResult absentSnapshot(
            ExecutionCheckpoint checkpoint,
            int expectedRevision,
            Instant occurredAt) {
        if (expectedRevision == -1) {
            return SaveReadResult.accepted(Optional.empty());
        }
        return SaveReadResult.rejected(conflict(checkpoint, expectedRevision, "missing", occurredAt));
    }

    private SaveReadResult malformedSnapshot(
            ExecutionCheckpoint checkpoint,
            String code,
            Instant occurredAt) {
        StoryDiagnostic diagnostic = diagnostics.create(code, checkpoint.state(), occurredAt, Map.of());
        return SaveReadResult.rejected(ExecutionSaveResult.failed(List.of(diagnostic)));
    }

    private SaveReadResult checkedRevision(
            ExecutionSnapshot snapshot,
            ExecutionCheckpoint checkpoint,
            int expectedRevision,
            Instant occurredAt) {
        if (expectedRevision == -1 || snapshot.executionRevision() != expectedRevision) {
            return SaveReadResult.rejected(conflict(
                    checkpoint,
                    expectedRevision,
                    Integer.toString(snapshot.executionRevision()),
                    occurredAt));
        }
        return SaveReadResult.accepted(Optional.of(snapshot));
    }

    private ExecutionSaveResult conflict(
            ExecutionCheckpoint checkpoint,
            int expectedRevision,
            String actualRevision,
            Instant occurredAt) {
        StoryDiagnostic diagnostic = diagnostics.create(
                "ESE-PERSISTENCE-010",
                checkpoint.state(),
                occurredAt,
                Map.of("expectedRevision", Integer.toString(expectedRevision), "actualRevision", actualRevision));
        return ExecutionSaveResult.revisionConflict(List.of(diagnostic));
    }

    private StoryDiagnostic saveFailed(
            ExecutionCheckpoint checkpoint,
            Instant occurredAt,
            Exception exception) {
        return diagnostics.create(
                "ESE-PERSISTENCE-001",
                checkpoint.state(),
                occurredAt,
                Map.of("failureCategory", exception.getClass().getSimpleName()));
    }

    private StoryDiagnostic diag(String code, ExecutionId executionId, Instant occurredAt) {
        return diagnostics.create(code, executionId, occurredAt, Map.of());
    }

    private record SaveReadResult(
            Optional<ExecutionSnapshot> snapshot,
            Optional<ExecutionSaveResult> rejected) {
        static SaveReadResult accepted(Optional<ExecutionSnapshot> snapshot) {
            return new SaveReadResult(snapshot, Optional.empty());
        }

        static SaveReadResult rejected(ExecutionSaveResult rejected) {
            return new SaveReadResult(Optional.empty(), Optional.of(rejected));
        }
    }
}
