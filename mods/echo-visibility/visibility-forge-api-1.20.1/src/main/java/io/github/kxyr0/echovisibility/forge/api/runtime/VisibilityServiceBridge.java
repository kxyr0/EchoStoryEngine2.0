package io.github.kxyr0.echovisibility.forge.api.runtime;

import io.github.kxyr0.echovisibility.forge.api.VisibilityQueryService;
import io.github.kxyr0.echovisibility.forge.api.VisibilityService;
import io.github.kxyr0.echovisibility.forge.api.VisibilityWatchCatalog;
import io.github.kxyr0.echovisibility.forge.api.concealment.VisibilityConcealmentView;
import io.github.kxyr0.echovisibility.forge.api.diagnostic.VisibilityDiagnosticsView;
import java.util.Objects;

public record VisibilityServiceBridge(
        VisibilityService service,
        VisibilityQueryService query,
        VisibilityWatchCatalog catalog,
        VisibilityConcealmentView concealment,
        VisibilityDiagnosticsView diagnostics) {
    public VisibilityServiceBridge {
        service = Objects.requireNonNull(service, "service");
        query = Objects.requireNonNull(query, "query");
        catalog = Objects.requireNonNull(catalog, "catalog");
        concealment = Objects.requireNonNull(concealment, "concealment");
        diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }
}
