package io.github.kxyr0.echoplayersleep.api.id;

import java.util.Objects;
import java.util.UUID;

public record PlayerSleepSessionId(String value) {
    public PlayerSleepSessionId {
        value = requireText(value, "sessionId");
    }

    public static PlayerSleepSessionId random() {
        return new PlayerSleepSessionId(UUID.randomUUID().toString());
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return checked;
    }
}
