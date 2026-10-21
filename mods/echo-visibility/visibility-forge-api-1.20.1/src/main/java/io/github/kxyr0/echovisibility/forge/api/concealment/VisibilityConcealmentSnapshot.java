package io.github.kxyr0.echovisibility.forge.api.concealment;

import io.github.kxyr0.echovisibility.api.identity.VisibilityConcealmentLeaseId;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchGeneration;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.api.state.VisibilityConcealmentStatus;
import java.util.Objects;

public record VisibilityConcealmentSnapshot(
        VisibilityConcealmentLeaseId leaseId,
        VisibilityWatchId watchId,
        VisibilityWatchGeneration generation,
        String npcId,
        VisibilityConcealmentStatus status,
        long deadlineTick) {
    public VisibilityConcealmentSnapshot {
        leaseId = Objects.requireNonNull(leaseId, "leaseId");
        watchId = Objects.requireNonNull(watchId, "watchId");
        generation = Objects.requireNonNull(generation, "generation");
        npcId = Objects.requireNonNull(npcId, "npcId");
        status = Objects.requireNonNull(status, "status");
    }
}
