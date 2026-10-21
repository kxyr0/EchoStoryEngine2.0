package io.github.kxyr0.echonpc.api.scene;

import java.util.Objects;

/**
 * Stable idempotency key for one authored scene command.
 */
public record NpcSceneCommandId(String value) {
    public NpcSceneCommandId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty() || value.length() > 128) {
            throw new IllegalArgumentException("NpcSceneCommandId must be non-empty and at most 128 characters.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
