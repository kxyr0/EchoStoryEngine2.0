package io.github.kxyr0.echonpc.api.scene;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import java.util.Objects;

/**
 * Public materialization readiness evidence for scene commands.
 */
public record NpcSceneReadinessView(
        ActorId npcId,
        boolean materialized,
        long materializationRevision,
        String barrier,
        String message
) {
    public NpcSceneReadinessView {
        npcId = Objects.requireNonNull(npcId, "npcId");
        barrier = Objects.requireNonNull(barrier, "barrier");
        message = Objects.requireNonNull(message, "message");
        if (materializationRevision < 0L) {
            throw new IllegalArgumentException("materializationRevision must not be negative.");
        }
    }
}
