package io.github.kxyr0.echostoryengine.story.definition.compile;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Explicit result of compiling a valid source model.
 */
public record StoryCompileResult(
        String sourceId,
        Optional<Integer> formatVersion,
        Optional<StoryDefinition> compiledStory,
        List<StoryDiagnostic> diagnostics) {
    public StoryCompileResult {
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        formatVersion = Objects.requireNonNull(formatVersion, "formatVersion");
        compiledStory = Objects.requireNonNull(compiledStory, "compiledStory");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public boolean succeeded() {
        return compiledStory.isPresent() && diagnostics.stream()
                .noneMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
    }
}
