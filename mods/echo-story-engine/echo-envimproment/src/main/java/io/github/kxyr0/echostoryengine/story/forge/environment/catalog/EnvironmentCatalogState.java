package io.github.kxyr0.echostoryengine.story.forge.environment.catalog;

import io.github.kxyr0.echostoryengine.story.forge.environment.resource.EnvironmentResourceCandidate;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomic holder of the latest immutable EnvironmentResource catalog.
 */
public final class EnvironmentCatalogState {
    private final AtomicReference<EnvironmentCatalogSnapshot> current;
    private final AtomicReference<EnvironmentCatalogReloadStatus> latestReload;

    public EnvironmentCatalogState(Instant startedAt) {
        current = new AtomicReference<>(EnvironmentCatalogSnapshot.empty(startedAt));
        latestReload = new AtomicReference<>(EnvironmentCatalogReloadStatus.notLoaded(startedAt));
    }

    public EnvironmentCatalogSnapshot current() {
        return current.get();
    }

    public EnvironmentCatalogReloadStatus latestReload() {
        return latestReload.get();
    }

    public EnvironmentCatalogInstallResult install(EnvironmentResourceCandidate candidate, Instant installedAt) {
        Objects.requireNonNull(candidate, "candidate");
        if (!candidate.accepted()) {
            latestReload.set(new EnvironmentCatalogReloadStatus(
                    "REJECTED", current().revision(), candidate.sourceCount(), candidate.errorCount(), installedAt));
            return new EnvironmentCatalogInstallResult(false, current(), candidate.diagnostics());
        }
        EnvironmentCatalogSnapshot previous = current();
        EnvironmentCatalogSnapshot next = new EnvironmentCatalogSnapshot(
                previous.revision() + 1, Objects.requireNonNull(installedAt, "installedAt"), candidate.entries());
        current.set(next);
        latestReload.set(new EnvironmentCatalogReloadStatus(
                "ACCEPTED", next.revision(), candidate.sourceCount(), candidate.errorCount(), installedAt));
        return new EnvironmentCatalogInstallResult(true, next, candidate.diagnostics());
    }
}
