package io.github.kxyr0.echoplayeractor.api.pose;

import io.github.kxyr0.echonavigation.api.position.Vector3;
import java.util.Objects;

public record PlayerActorTransform(
        String dimensionId,
        Vector3 position,
        double bodyYaw,
        double headYaw,
        double pitch) {
    public PlayerActorTransform {
        dimensionId = Objects.requireNonNull(dimensionId, "dimensionId").trim();
        position = Objects.requireNonNull(position, "position");
        if (dimensionId.isEmpty() || !dimensionId.contains(":")) {
            throw new IllegalArgumentException("dimensionId must be namespaced.");
        }
        requireFinite(bodyYaw, "bodyYaw");
        requireFinite(headYaw, "headYaw");
        requireFinite(pitch, "pitch");
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }
}
