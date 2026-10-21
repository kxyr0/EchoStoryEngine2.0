package io.github.kxyr0.echostoryengine.story.definition.compile;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentResourceDef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of compiling one EnvironmentResource source.
 */
public record EnvironmentCompilationResult(
        boolean parsingSucceeded,
        boolean bindingSucceeded,
        boolean validationSucceeded,
        String sourceId,
        Optional<EnvironmentResourceDef> environment,
        List<StoryDiagnostic> diagnostics) {
    public EnvironmentCompilationResult {
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        environment = Objects.requireNonNull(environment, "environment");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public boolean compilationSucceeded() {
        return parsingSucceeded && bindingSucceeded && validationSucceeded && environment.isPresent();
    }
}
