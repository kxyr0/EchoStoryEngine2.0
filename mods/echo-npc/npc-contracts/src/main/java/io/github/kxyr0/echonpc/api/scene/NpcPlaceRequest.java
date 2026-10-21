package io.github.kxyr0.echonpc.api.scene;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import java.util.Objects;
import java.util.Optional;

public record NpcPlaceRequest(
        ActorId npcId,
        NpcWorldId worldId,
        NpcPosition position,
        NpcRotation rotation,
        Optional<NpcSceneCommandId> commandId
) {
    public NpcPlaceRequest {
        npcId = Objects.requireNonNull(npcId, "npcId");
        worldId = Objects.requireNonNull(worldId, "worldId");
        position = Objects.requireNonNull(position, "position");
        rotation = Objects.requireNonNull(rotation, "rotation");
        commandId = Objects.requireNonNull(commandId, "commandId");
    }

    public NpcPlaceRequest(ActorId npcId, NpcWorldId worldId, NpcPosition position, NpcRotation rotation) {
        this(npcId, worldId, position, rotation, Optional.empty());
    }
}
