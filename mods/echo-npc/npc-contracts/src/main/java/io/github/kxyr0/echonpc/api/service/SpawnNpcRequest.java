package io.github.kxyr0.echonpc.api.service;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable public spawn request. ActorId is caller-supplied and never generated from entity identity.
 */
public record SpawnNpcRequest(
        ActorId actorId,
        NpcDefinitionId definitionId,
        NpcWorldId worldId,
        NpcPosition position,
        NpcRotation rotation,
        Optional<ActorMutationId> mutationId
) {
    public SpawnNpcRequest {
        actorId = Objects.requireNonNull(actorId, "actorId");
        definitionId = Objects.requireNonNull(definitionId, "definitionId");
        worldId = Objects.requireNonNull(worldId, "worldId");
        position = Objects.requireNonNull(position, "position");
        rotation = Objects.requireNonNull(rotation, "rotation");
        mutationId = Objects.requireNonNull(mutationId, "mutationId");
    }

    public SpawnNpcRequest(ActorId actorId, NpcDefinitionId definitionId, NpcWorldId worldId,
            NpcPosition position, NpcRotation rotation) {
        this(actorId, definitionId, worldId, position, rotation, Optional.empty());
    }

    public SpawnNpcRequest(ActorId actorId, NpcDefinitionId definitionId, NpcWorldId worldId,
            NpcPosition position, NpcRotation rotation, ActorMutationId mutationId) {
        this(actorId, definitionId, worldId, position, rotation,
                Optional.of(Objects.requireNonNull(mutationId, "mutationId")));
    }
}
