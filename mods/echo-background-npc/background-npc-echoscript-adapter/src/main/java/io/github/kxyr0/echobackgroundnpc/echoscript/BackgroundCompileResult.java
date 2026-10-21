package io.github.kxyr0.echobackgroundnpc.echoscript;

import io.github.kxyr0.echobackgroundnpc.api.diagnostic.BackgroundDiagnostic;
import io.github.kxyr0.echobackgroundnpc.core.catalog.BackgroundCatalog;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record BackgroundCompileResult(Optional<BackgroundCatalog> catalog,
        List<BackgroundDiagnostic> diagnostics) {
    public BackgroundCompileResult {
        catalog = Objects.requireNonNull(catalog, "catalog");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }
}
