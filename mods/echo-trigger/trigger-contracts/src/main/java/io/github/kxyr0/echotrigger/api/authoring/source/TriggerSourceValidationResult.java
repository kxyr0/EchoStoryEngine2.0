package io.github.kxyr0.echotrigger.api.authoring.source;

import io.github.kxyr0.echotrigger.api.authoring.catalog.AreaDiagnosticView;
import io.github.kxyr0.echotrigger.api.authoring.catalog.TriggerAreaDefinitionView;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record TriggerSourceValidationResult(
        boolean successful,
        int parserCalls,
        int binderCalls,
        String sourceFingerprint,
        Optional<TriggerAreaDefinitionView> compiledArea,
        List<AreaDiagnosticView> diagnostics
) {
    public TriggerSourceValidationResult {
        sourceFingerprint = Objects.requireNonNull(sourceFingerprint, "sourceFingerprint");
        compiledArea = Objects.requireNonNull(compiledArea, "compiledArea");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        if (parserCalls < 0 || binderCalls < 0) {
            throw new IllegalArgumentException("Call counters must not be negative.");
        }
    }
}
