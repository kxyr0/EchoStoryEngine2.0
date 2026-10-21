package io.github.kxyr0.echostorynpcadapter.action;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import java.util.Objects;

public record TeleportPayload(
        ActorId actorId,
        NpcWorldId worldId,
        NpcPosition position,
        NpcRotation rotation) {
    public TeleportPayload {
        actorId = Objects.requireNonNull(actorId, "actorId");
        worldId = Objects.requireNonNull(worldId, "worldId");
        position = Objects.requireNonNull(position, "position");
        rotation = Objects.requireNonNull(rotation, "rotation");
    }
}
