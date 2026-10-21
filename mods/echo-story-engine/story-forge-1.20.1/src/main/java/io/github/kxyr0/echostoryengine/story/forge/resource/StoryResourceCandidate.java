package io.github.kxyr0.echostoryengine.story.forge.resource;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.CompiledStoryCatalogEntry;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Полностью собранный candidate catalog; invalid candidate не заменяет текущий catalog.
 */
public record StoryResourceCandidate(
        boolean accepted,
        Map<StoryId, CompiledStoryCatalogEntry> entries,
        List<StoryDiagnostic> diagnostics,
        int sourceCount,
        int errorCount) {
    public StoryResourceCandidate {
        entries = Map.copyOf(Objects.requireNonNull(entries, "entries"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        if (sourceCount < 0 || errorCount < 0) {
            throw new IllegalArgumentException("counts must be non-negative");
        }
    }
}
