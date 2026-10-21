package io.github.kxyr0.echostoryengine.story.persistence.file;

import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Reads persisted snapshot identities for server-start recovery.
 */
final class ExecutionSnapshotIndex {
    private final SnapshotFileReader reader;

    ExecutionSnapshotIndex(SnapshotFileReader reader) {
        this.reader = reader;
    }

    List<ExecutionId> list(Path rootDirectory) {
        if (!Files.isDirectory(rootDirectory)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(rootDirectory)) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .map(reader::read)
                    .flatMap(read -> read.checkpoint().stream())
                    .map(checkpoint -> checkpoint.state().executionId())
                    .distinct()
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("execution id listing failed", exception);
        }
    }
}
