package io.github.kxyr0.echostoryengine.story.echoscript.binding;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceStory;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of binding EchoScript syntax to the StoryResource source model.
 */
public record StoryResourceBinding(Optional<SourceStory> sourceStory, List<StoryDiagnostic> diagnostics) {
    public StoryResourceBinding {
        sourceStory = Objects.requireNonNull(sourceStory, "sourceStory");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public boolean succeeded() {
        return sourceStory.isPresent()
                && diagnostics.stream().noneMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
    }
}
