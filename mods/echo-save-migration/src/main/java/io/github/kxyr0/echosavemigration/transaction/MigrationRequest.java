package io.github.kxyr0.echosavemigration.transaction;

import io.github.kxyr0.echosavemigration.api.SaveSchemaVersion;
import java.nio.file.Path;
import java.util.Objects;

public record MigrationRequest(
        Path source,
        Path backupRoot,
        Path journal,
        SaveSchemaVersion runtimeSchema,
        boolean dryRun
) {
    public MigrationRequest {
        source = source.toAbsolutePath().normalize();
        backupRoot = backupRoot.toAbsolutePath().normalize();
        journal = journal.toAbsolutePath().normalize();
        runtimeSchema = Objects.requireNonNull(runtimeSchema, "runtimeSchema");
    }
}
