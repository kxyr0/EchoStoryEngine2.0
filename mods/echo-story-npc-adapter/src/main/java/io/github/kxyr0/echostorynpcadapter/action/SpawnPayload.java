package io.github.kxyr0.echostorynpcadapter.action;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import java.util.Objects;

public record SpawnPayload(
        ActorId actorId,
        NpcDefinitionId definitionId,
        NpcWorldId worldId,
        NpcPosition position,
        NpcRotation rotation) {
    public SpawnPayload {
        actorId = Objects.requireNonNull(actorId, "actorId");
        definitionId = Objects.requireNonNull(definitionId, "definitionId");
        worldId = Objects.requireNonNull(worldId, "worldId");
        position = Objects.requireNonNull(position, "position");
        rotation = Objects.requireNonNull(rotation, "rotation");
    }
}
