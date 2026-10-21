package io.github.kxyr0.echoplayeractor.core.rotation;

import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echoplayeractor.api.pose.PlayerActorTransform;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorLookRequest;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorRotationRequest;
import java.util.Objects;

public final class PlayerLookAtResolver {
    private static final double EYE_HEIGHT = 1.62D;
    private static final double MIN_DISTANCE = 0.0001D;

    public PlayerActorRotationRequest resolve(PlayerActorTransform origin, PlayerActorLookRequest request) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(request, "request");
        Vector3 eye = new Vector3(origin.position().x(), origin.position().y() + EYE_HEIGHT, origin.position().z());
        Vector3 target = request.target();
        double dx = target.x() - eye.x();
        double dy = target.y() - eye.y();
        double dz = target.z() - eye.z();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (Math.sqrt(dx * dx + dy * dy + dz * dz) <= MIN_DISTANCE) {
            throw new IllegalArgumentException("LookAt target is too close.");
        }
        double yaw = AngleMath.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        double pitch = AngleMath.clampPitch(-Math.toDegrees(Math.atan2(dy, horizontal)));
        return new PlayerActorRotationRequest(request.commandId(), request.actorId(), request.ownerId(), yaw, pitch,
                request.durationTicks(), request.interpolation());
    }
}
