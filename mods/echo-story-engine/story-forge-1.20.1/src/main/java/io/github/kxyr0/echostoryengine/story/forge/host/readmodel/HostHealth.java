package io.github.kxyr0.echostoryengine.story.forge.host.readmodel;

import io.github.kxyr0.echostoryengine.story.forge.host.ForgeServerHostState;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.CatalogReloadStatus;

/**
 * Read-only health view for diagnostics command.
 */
public record HostHealth(
        ForgeServerHostState state,
        long catalogRevision,
        int catalogStories,
        CatalogReloadStatus latestReload,
        int activeExecutions,
        int waitingExecutions,
        int pendingPersistenceOperations,
        int persistenceQueueCapacity,
        int pendingServerWork,
        int serverWorkQueueCapacity) {
}
