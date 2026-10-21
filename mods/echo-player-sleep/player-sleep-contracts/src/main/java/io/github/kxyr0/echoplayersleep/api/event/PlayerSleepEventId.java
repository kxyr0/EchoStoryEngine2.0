package io.github.kxyr0.echoplayersleep.api.event;

import java.util.Objects;

public record PlayerSleepEventId(String value) {
    public PlayerSleepEventId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("PlayerSleepEventId must not be blank.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
