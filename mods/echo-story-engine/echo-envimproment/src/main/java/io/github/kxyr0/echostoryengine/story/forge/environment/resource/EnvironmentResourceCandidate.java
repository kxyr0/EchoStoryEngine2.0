package io.github.kxyr0.echostoryengine.story.forge.environment.resource;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.forge.environment.catalog.EnvironmentCatalogEntry;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Compiled EnvironmentResource catalog candidate.
 */
public record EnvironmentResourceCandidate(
        boolean accepted,
        Map<NamespacedId, EnvironmentCatalogEntry> entries,
        List<StoryDiagnostic> diagnostics,
        int sourceCount,
        int errorCount) {
    public EnvironmentResourceCandidate {
        entries = Map.copyOf(Objects.requireNonNull(entries, "entries"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        if (sourceCount < 0 || errorCount < 0) {
            throw new IllegalArgumentException("counts must be non-negative");
        }
    }
}
