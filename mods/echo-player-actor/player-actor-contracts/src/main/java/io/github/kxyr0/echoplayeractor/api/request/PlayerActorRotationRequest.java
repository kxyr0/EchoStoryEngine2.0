package io.github.kxyr0.echoplayeractor.api.request;

import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import java.util.Objects;

public record PlayerActorRotationRequest(
        PlayerActorCommandId commandId,
        PlayerActorId actorId,
        PlayerActorOwnerId ownerId,
        double yaw,
        double pitch,
        int durationTicks,
        PlayerActorInterpolation interpolation) {
    public PlayerActorRotationRequest {
        commandId = Objects.requireNonNull(commandId, "commandId");
        actorId = Objects.requireNonNull(actorId, "actorId");
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        interpolation = Objects.requireNonNull(interpolation, "interpolation");
        requireFinite(yaw, "yaw");
        requireFinite(pitch, "pitch");
        if (durationTicks < 0) {
            throw new IllegalArgumentException("durationTicks must not be negative.");
        }
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }
}
