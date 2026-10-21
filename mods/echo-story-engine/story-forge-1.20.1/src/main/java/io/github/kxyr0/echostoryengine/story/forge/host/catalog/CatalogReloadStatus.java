package io.github.kxyr0.echostoryengine.story.forge.host.catalog;

import java.time.Instant;
import java.util.Objects;

/**
 * Read-only summary of the latest story resource reload.
 */
public record CatalogReloadStatus(
        String status,
        long catalogRevision,
        int sourceCount,
        int errorCount,
        Instant occurredAt) {
    public CatalogReloadStatus {
        status = Objects.requireNonNull(status, "status");
        if (catalogRevision < 0 || sourceCount < 0 || errorCount < 0) {
            throw new IllegalArgumentException("counts must be non-negative");
        }
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
    }

    public static CatalogReloadStatus notLoaded(Instant occurredAt) {
        return new CatalogReloadStatus("NOT_LOADED", 0, 0, 0, occurredAt);
    }
}
