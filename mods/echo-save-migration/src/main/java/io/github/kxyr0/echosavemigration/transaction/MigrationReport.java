package io.github.kxyr0.echosavemigration.transaction;

import io.github.kxyr0.echosavemigration.api.MigrationId;
import io.github.kxyr0.echosavemigration.api.SaveSchemaVersion;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public record MigrationReport(
        SaveSchemaVersion from,
        SaveSchemaVersion to,
        List<MigrationId> migrations,
        int operations,
        Optional<Path> backup,
        Status status
) {
    public MigrationReport {
        migrations = List.copyOf(migrations);
        backup = backup.map(value -> value.toAbsolutePath().normalize());
    }

    public enum Status {
        CURRENT,
        DRY_RUN,
        COMMITTED,
        RECOVERED
    }
}
