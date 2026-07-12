package io.github.kxyr0.echostoryengine.story.definition.parse;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceStory;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Explicit result of syntax parsing.
 */
public record StoryParseResult(
        String sourceId,
        Optional<Integer> formatVersion,
        Optional<SourceStory> source,
        List<StoryDiagnostic> diagnostics) {
    public StoryParseResult {
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        formatVersion = Objects.requireNonNull(formatVersion, "formatVersion");
        source = Objects.requireNonNull(source, "source");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public boolean succeeded() {
        return source.isPresent() && diagnostics.stream()
                .noneMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
    }
}
