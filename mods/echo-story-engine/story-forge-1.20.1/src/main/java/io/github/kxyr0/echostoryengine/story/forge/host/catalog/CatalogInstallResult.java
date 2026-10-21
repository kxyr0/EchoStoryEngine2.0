package io.github.kxyr0.echostoryengine.story.forge.host.catalog;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import java.util.List;
import java.util.Objects;

/**
 * Результат попытки atomic install каталога.
 */
public record CatalogInstallResult(
        boolean installed,
        CompiledStoryCatalogSnapshot current,
        List<StoryDiagnostic> diagnostics) {
    public CatalogInstallResult {
        current = Objects.requireNonNull(current, "current");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }
}
