package io.github.kxyr0.echostoryengine.story.forge.environment.catalog;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import java.util.List;
import java.util.Objects;

/**
 * Result of publishing or rejecting an EnvironmentResource catalog candidate.
 */
public record EnvironmentCatalogInstallResult(
        boolean installed,
        EnvironmentCatalogSnapshot current,
        List<StoryDiagnostic> diagnostics) {
    public EnvironmentCatalogInstallResult {
        current = Objects.requireNonNull(current, "current");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }
}
