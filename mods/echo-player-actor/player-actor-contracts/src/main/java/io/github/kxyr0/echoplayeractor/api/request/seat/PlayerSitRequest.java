package io.github.kxyr0.echoplayeractor.api.request.seat;

import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import io.github.kxyr0.echoplayeractor.api.position.BlockPosition;
import java.util.Objects;

public record PlayerSitRequest(
        PlayerActorCommandId commandId,
        PlayerActorId actorId,
        PlayerActorOwnerId ownerId,
        BlockPosition chairPosition,
        int timeoutTicks) {
    public PlayerSitRequest {
        commandId = Objects.requireNonNull(commandId, "commandId");
        actorId = Objects.requireNonNull(actorId, "actorId");
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        chairPosition = Objects.requireNonNull(chairPosition, "chairPosition");
        if (timeoutTicks < 1) {
            throw new IllegalArgumentException("timeoutTicks must be positive.");
        }
    }
}
