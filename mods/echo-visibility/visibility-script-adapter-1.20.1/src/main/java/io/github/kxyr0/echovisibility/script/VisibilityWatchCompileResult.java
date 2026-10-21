package io.github.kxyr0.echovisibility.script;

import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnostic;
import io.github.kxyr0.echovisibility.api.model.VisibilityWatchDefinition;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record VisibilityWatchCompileResult(
        Optional<VisibilityWatchDefinition> definition,
        List<VisibilityDiagnostic> diagnostics,
        int parserCalls,
        int binderCalls) {
    public VisibilityWatchCompileResult {
        definition = Objects.requireNonNull(definition, "definition");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        if (parserCalls < 0 || binderCalls < 0) {
            throw new IllegalArgumentException("Compile counters must not be negative.");
        }
    }

    public boolean successful() {
        return definition.isPresent() && diagnostics.isEmpty();
    }
}
