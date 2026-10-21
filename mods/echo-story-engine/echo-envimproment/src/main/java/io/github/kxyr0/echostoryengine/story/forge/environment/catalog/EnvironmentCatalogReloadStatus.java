package io.github.kxyr0.echostoryengine.story.forge.environment.catalog;

import java.time.Instant;

public record EnvironmentCatalogReloadStatus(
        String status,
        long revision,
        int sourceCount,
        int errorCount,
        Instant occurredAt) {
    public static EnvironmentCatalogReloadStatus notLoaded(Instant occurredAt) {
        return new EnvironmentCatalogReloadStatus("NOT_LOADED", 0, 0, 0, occurredAt);
    }
}
