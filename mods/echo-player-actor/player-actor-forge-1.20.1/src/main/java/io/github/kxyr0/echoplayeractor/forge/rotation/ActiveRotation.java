package io.github.kxyr0.echoplayeractor.forge.rotation;

import io.github.kxyr0.echoplayeractor.api.request.PlayerActorRotationRequest;
import java.util.Objects;

final class ActiveRotation {
    private final PlayerActorRotationRequest request;
    private final double startYaw;
    private final double startPitch;
    private int elapsedTicks;

    ActiveRotation(PlayerActorRotationRequest request, double startYaw, double startPitch) {
        this.request = Objects.requireNonNull(request, "request");
        this.startYaw = startYaw;
        this.startPitch = startPitch;
    }

    PlayerActorRotationRequest request() {
        return request;
    }

    double startYaw() {
        return startYaw;
    }

    double startPitch() {
        return startPitch;
    }

    int advance() {
        elapsedTicks += 1;
        return elapsedTicks;
    }

    int elapsedTicks() {
        return elapsedTicks;
    }
}
