package io.github.kxyr0.echonpc.core.platform;

import java.util.Objects;

/**
 * Opaque platform key for the current visible entity. It is never a public ActorId authority.
 */
public record PlatformActorHandle(String value) {
    public PlatformActorHandle {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("PlatformActorHandle must not be empty.");
        }
    }
}
