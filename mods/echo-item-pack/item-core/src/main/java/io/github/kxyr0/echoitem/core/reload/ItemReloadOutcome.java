package io.github.kxyr0.echoitem.core.reload;

import io.github.kxyr0.echoitem.api.diagnostic.ItemDefinitionDiagnostic;
import io.github.kxyr0.echoitem.core.catalog.ItemCatalogSnapshot;
import java.util.List;
import java.util.Objects;

/**
 * Immutable result of a server-side item reload batch.
 */
public record ItemReloadOutcome(
        ItemCatalogSnapshot snapshot,
        List<ItemDefinitionDiagnostic> diagnostics,
        ReloadCounters counters,
        boolean applied
) {
    public ItemReloadOutcome {
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        counters = Objects.requireNonNull(counters, "counters");
    }
}
