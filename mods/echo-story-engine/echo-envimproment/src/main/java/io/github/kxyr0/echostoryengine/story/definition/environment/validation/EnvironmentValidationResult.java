package io.github.kxyr0.echostoryengine.story.definition.environment.validation;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import java.util.List;
import java.util.Objects;

/**
 * Diagnostics returned by EnvironmentResource validation.
 */
public record EnvironmentValidationResult(List<StoryDiagnostic> diagnostics) {
    public EnvironmentValidationResult {
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public boolean succeeded() {
        return diagnostics.isEmpty();
    }
}
