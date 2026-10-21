package io.github.kxyr0.echosavemigration.definition;

import io.github.kxyr0.echosavemigration.api.MigrationId;
import io.github.kxyr0.echosavemigration.api.SaveSchemaVersion;
import java.util.List;
import java.util.Objects;

public record MigrationDefinition(
        MigrationId id,
        SaveSchemaVersion from,
        SaveSchemaVersion to,
        long revision,
        List<MigrationOperation> operations
) {
    public MigrationDefinition {
        id = Objects.requireNonNull(id, "id");
        from = Objects.requireNonNull(from, "from");
        to = Objects.requireNonNull(to, "to");
        operations = List.copyOf(operations);
        if (to.compareTo(from) <= 0 || revision < 1) {
            throw new IllegalArgumentException("Migration must advance schema with positive revision.");
        }
    }
}
