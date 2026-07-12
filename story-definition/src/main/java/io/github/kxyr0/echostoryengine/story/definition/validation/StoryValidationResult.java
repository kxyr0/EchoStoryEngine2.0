package io.github.kxyr0.echostoryengine.story.definition.validation;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Explicit result of one validation stage.
 */
public record StoryValidationResult(
        String sourceId,
        Optional<Integer> formatVersion,
        List<StoryDiagnostic> diagnostics) {
    public StoryValidationResult {
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        formatVersion = Objects.requireNonNull(formatVersion, "formatVersion");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public boolean succeeded() {
        return diagnostics.stream().noneMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
    }
}
