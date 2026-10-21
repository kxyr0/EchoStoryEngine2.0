package io.github.kxyr0.echoitem.core.asset;

import io.github.kxyr0.echoitem.api.diagnostic.ItemDefinitionDiagnostic;
import io.github.kxyr0.echoitem.core.reload.ReloadCounters;
import java.util.List;
import java.util.Objects;

/**
 * Immutable texture or model validation result.
 */
public record AssetValidationResult(
        boolean valid,
        List<ItemDefinitionDiagnostic> diagnostics,
        ReloadCounters counters
) {
    public AssetValidationResult {
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        counters = Objects.requireNonNull(counters, "counters");
    }
}
