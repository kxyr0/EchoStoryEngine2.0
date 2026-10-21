package io.github.kxyr0.echoteleportmagic.api;

import java.util.Objects;

/** Resolved location; authoring uses the host's existing Location catalog. */
public record TeleportDestination(String locationId, String dimension, double x, double y, double z,
                                  float yaw, float pitch) {
    public TeleportDestination {
        Objects.requireNonNull(locationId);
        Objects.requireNonNull(dimension);
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("Non-finite destination");
        }
    }
}
