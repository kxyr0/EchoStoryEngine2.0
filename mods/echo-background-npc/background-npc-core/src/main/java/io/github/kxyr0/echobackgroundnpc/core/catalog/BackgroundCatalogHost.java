package io.github.kxyr0.echobackgroundnpc.core.catalog;

import io.github.kxyr0.echobackgroundnpc.api.diagnostic.BackgroundDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Last-known-good catalog holder; failed candidates never disturb active agents. */
public final class BackgroundCatalogHost {
    private BackgroundCatalog current = BackgroundCatalog.of(0L, List.of(), List.of(), List.of(), List.of());

    public synchronized boolean commit(Optional<BackgroundCatalog> candidate,
            List<BackgroundDiagnostic> diagnostics) {
        Objects.requireNonNull(candidate, "candidate");
        if (candidate.isEmpty() || diagnostics.stream().anyMatch(BackgroundDiagnostic::error)) return false;
        current = candidate.orElseThrow();
        return true;
    }

    public synchronized BackgroundCatalog current() { return current; }
}
