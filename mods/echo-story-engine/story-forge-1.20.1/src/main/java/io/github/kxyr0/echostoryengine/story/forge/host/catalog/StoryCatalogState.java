package io.github.kxyr0.echostoryengine.story.forge.host.catalog;

import io.github.kxyr0.echostoryengine.story.forge.resource.StoryResourceCandidate;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomic holder текущего immutable каталога story resources.
 */
public final class StoryCatalogState {
    private final AtomicReference<CompiledStoryCatalogSnapshot> current;
    private final AtomicReference<CatalogReloadStatus> latestReload;

    public StoryCatalogState(Instant startedAt) {
        this.current = new AtomicReference<>(CompiledStoryCatalogSnapshot.empty(startedAt));
        this.latestReload = new AtomicReference<>(CatalogReloadStatus.notLoaded(startedAt));
    }

    public CompiledStoryCatalogSnapshot current() {
        return current.get();
    }

    public CatalogReloadStatus latestReload() {
        return latestReload.get();
    }

    public CatalogInstallResult install(StoryResourceCandidate candidate, Instant installedAt) {
        Objects.requireNonNull(candidate, "candidate");
        if (!candidate.accepted()) {
            latestReload.set(new CatalogReloadStatus(
                    "REJECTED",
                    current().revision(),
                    candidate.sourceCount(),
                    candidate.errorCount(),
                    installedAt));
            return new CatalogInstallResult(false, current(), candidate.diagnostics());
        }
        CompiledStoryCatalogSnapshot previous = current();
        CompiledStoryCatalogSnapshot next = new CompiledStoryCatalogSnapshot(
                previous.revision() + 1,
                Objects.requireNonNull(installedAt, "installedAt"),
                candidate.entries());
        current.set(next);
        latestReload.set(new CatalogReloadStatus(
                "ACCEPTED",
                next.revision(),
                candidate.sourceCount(),
                candidate.errorCount(),
                installedAt));
        return new CatalogInstallResult(true, next, candidate.diagnostics());
    }
}
