package io.github.kxyr0.echosavemigration.compile;

import io.github.kxyr0.echosavemigration.definition.MigrationDefinition;
import java.util.List;
import java.util.Optional;

public record MigrationCompileResult(Optional<MigrationDefinition> definition, List<MigrationDiagnostic> diagnostics) {
    public MigrationCompileResult {
        diagnostics = List.copyOf(diagnostics);
    }

    public boolean successful() {
        return definition.isPresent() && diagnostics.isEmpty();
    }
}
