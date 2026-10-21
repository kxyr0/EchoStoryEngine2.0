package io.github.kxyr0.echostoryengine.story.forge.resource.discovery;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.forge.resource.StoryResourceText;
import java.util.List;
import java.util.Objects;

/**
 * Text resources plus diagnostics collected while reading Forge data resources.
 */
public record StoryResourceReadResult(
        List<StoryResourceText> resources,
        List<StoryDiagnostic> diagnostics) {
    public StoryResourceReadResult {
        resources = List.copyOf(Objects.requireNonNull(resources, "resources"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }
}
