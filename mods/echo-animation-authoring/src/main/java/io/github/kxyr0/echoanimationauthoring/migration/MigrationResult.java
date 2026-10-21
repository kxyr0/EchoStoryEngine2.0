package io.github.kxyr0.echoanimationauthoring.migration;

import java.util.List;
import java.util.Objects;

/**
 * Result of legacy facial animation migration.
 */
public record MigrationResult(String echoScript, List<String> warnings) {
    public MigrationResult {
        echoScript = Objects.requireNonNull(echoScript, "echoScript");
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings"));
    }
}

