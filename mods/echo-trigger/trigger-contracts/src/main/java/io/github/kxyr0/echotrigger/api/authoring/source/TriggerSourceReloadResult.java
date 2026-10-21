package io.github.kxyr0.echotrigger.api.authoring.source;

import io.github.kxyr0.echotrigger.api.authoring.catalog.AreaDiagnosticView;
import io.github.kxyr0.echotrigger.api.authoring.catalog.AreaSourceLocation;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record TriggerSourceReloadResult(
        boolean successful,
        long catalogRevisionBefore,
        long catalogRevisionAfter,
        TriggerId expectedId,
        Optional<AreaSourceLocation> activeSource,
        List<AreaDiagnosticView> diagnostics
) {
    public TriggerSourceReloadResult {
        expectedId = Objects.requireNonNull(expectedId, "expectedId");
        activeSource = Objects.requireNonNull(activeSource, "activeSource");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        if (catalogRevisionBefore < 0 || catalogRevisionAfter < 0) {
            throw new IllegalArgumentException("Catalog revisions must not be negative.");
        }
    }
}
