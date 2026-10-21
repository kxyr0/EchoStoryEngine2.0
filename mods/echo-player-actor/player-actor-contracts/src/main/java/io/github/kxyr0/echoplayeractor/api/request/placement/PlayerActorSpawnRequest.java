package io.github.kxyr0.echoplayeractor.api.request.placement;

import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import java.util.Objects;

public record PlayerActorSpawnRequest(
        PlayerActorCommandId commandId,
        PlayerActorId actorId,
        PlayerActorOwnerId ownerId,
        Vector3 position,
        double yaw,
        double pitch) {
    public PlayerActorSpawnRequest {
        commandId = Objects.requireNonNull(commandId, "commandId");
        actorId = Objects.requireNonNull(actorId, "actorId");
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        position = Objects.requireNonNull(position, "position");
        requireFinite(position.x(), "position.x");
        requireFinite(position.y(), "position.y");
        requireFinite(position.z(), "position.z");
        requireFinite(yaw, "yaw");
        requireFinite(pitch, "pitch");
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }
}
