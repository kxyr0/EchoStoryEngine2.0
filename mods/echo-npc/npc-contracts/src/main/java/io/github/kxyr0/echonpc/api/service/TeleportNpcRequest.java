package io.github.kxyr0.echonpc.api.service;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable immediate teleport request. No route or interpolation is implied.
 */
public record TeleportNpcRequest(
        ActorId actorId,
        NpcWorldId worldId,
        NpcPosition position,
        NpcRotation rotation,
        Optional<ActorMutationId> mutationId
) {
    public TeleportNpcRequest {
        actorId = Objects.requireNonNull(actorId, "actorId");
        worldId = Objects.requireNonNull(worldId, "worldId");
        position = Objects.requireNonNull(position, "position");
        rotation = Objects.requireNonNull(rotation, "rotation");
        mutationId = Objects.requireNonNull(mutationId, "mutationId");
    }

    public TeleportNpcRequest(ActorId actorId, NpcWorldId worldId, NpcPosition position,
            NpcRotation rotation) {
        this(actorId, worldId, position, rotation, Optional.empty());
    }

    public TeleportNpcRequest(ActorId actorId, NpcWorldId worldId, NpcPosition position,
            NpcRotation rotation, ActorMutationId mutationId) {
        this(actorId, worldId, position, rotation, Optional.of(Objects.requireNonNull(mutationId, "mutationId")));
    }
}
