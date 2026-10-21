package io.github.kxyr0.echonpc.api.rest.command;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable wake-up request.
 */
public record NpcWakeRequest(ActorId npcId, Optional<ActorMutationId> mutationId) {
    public NpcWakeRequest {
        npcId = Objects.requireNonNull(npcId, "npcId");
        mutationId = Objects.requireNonNull(mutationId, "mutationId");
    }

    public NpcWakeRequest(ActorId npcId) {
        this(npcId, Optional.empty());
    }

    public NpcWakeRequest(ActorId npcId, ActorMutationId mutationId) {
        this(npcId, Optional.of(Objects.requireNonNull(mutationId, "mutationId")));
    }
}
