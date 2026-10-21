package io.github.kxyr0.echoplayeractor.api.request;

import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import java.util.Objects;

public record PlayerActorLookRequest(
        PlayerActorCommandId commandId,
        PlayerActorId actorId,
        PlayerActorOwnerId ownerId,
        Vector3 target,
        int durationTicks,
        PlayerActorInterpolation interpolation,
        PlayerActorLookMode mode) {
    public PlayerActorLookRequest {
        commandId = Objects.requireNonNull(commandId, "commandId");
        actorId = Objects.requireNonNull(actorId, "actorId");
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        target = Objects.requireNonNull(target, "target");
        interpolation = Objects.requireNonNull(interpolation, "interpolation");
        mode = Objects.requireNonNull(mode, "mode");
        if (durationTicks < 0) {
            throw new IllegalArgumentException("durationTicks must not be negative.");
        }
    }
}
