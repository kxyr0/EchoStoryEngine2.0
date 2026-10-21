package io.github.kxyr0.echoplayeractor.api.request;

import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import java.util.Objects;

public record PlayerActorMoveRequest(
        PlayerActorCommandId commandId,
        PlayerActorId actorId,
        PlayerActorOwnerId ownerId,
        Vector3 destination,
        String dimensionId,
        int timeoutTicks) {
    public PlayerActorMoveRequest {
        commandId = Objects.requireNonNull(commandId, "commandId");
        actorId = Objects.requireNonNull(actorId, "actorId");
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        destination = Objects.requireNonNull(destination, "destination");
        dimensionId = Objects.requireNonNull(dimensionId, "dimensionId").trim();
        if (dimensionId.isEmpty() || !dimensionId.contains(":")) {
            throw new IllegalArgumentException("dimensionId must be namespaced.");
        }
        if (timeoutTicks < 1) {
            throw new IllegalArgumentException("timeoutTicks must be positive.");
        }
    }
}
