package io.github.kxyr0.echonpc.api.appearance;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.identity.NpcSkinId;
import java.util.Objects;
import java.util.Optional;

/**
 * Requests an atomic change of the texture used by one durable logical actor.
 */
public record NpcSkinChangeRequest(
        ActorId npcId,
        NpcSkinId skinId,
        Optional<ActorMutationId> mutationId
) {
    public NpcSkinChangeRequest {
        npcId = Objects.requireNonNull(npcId, "npcId");
        skinId = Objects.requireNonNull(skinId, "skinId");
        mutationId = Objects.requireNonNull(mutationId, "mutationId");
    }

    public NpcSkinChangeRequest(ActorId npcId, NpcSkinId skinId) {
        this(npcId, skinId, Optional.empty());
    }

    public NpcSkinChangeRequest(ActorId npcId, NpcSkinId skinId, ActorMutationId mutationId) {
        this(npcId, skinId, Optional.of(Objects.requireNonNull(mutationId, "mutationId")));
    }
}
