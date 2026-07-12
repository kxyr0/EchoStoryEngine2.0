package io.github.kxyr0.echostoryengine.story.definition.compile;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Full source pipeline result with explicit stage success flags.
 */
public record StoryCompilationResult(
        boolean parsingSucceeded,
        boolean structuralValidationSucceeded,
        boolean semanticValidationSucceeded,
        boolean compilationSucceeded,
        String sourceId,
        Optional<Integer> formatVersion,
        Optional<StoryDefinition> compiledStory,
        List<StoryDiagnostic> diagnostics) {
    public StoryCompilationResult {
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        formatVersion = Objects.requireNonNull(formatVersion, "formatVersion");
        compiledStory = Objects.requireNonNull(compiledStory, "compiledStory");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }
}
