package io.github.kxyr0.echoplayersleep.api.id;

import java.util.Objects;

public record PlayerSleepOwnerId(String value) {
    public PlayerSleepOwnerId {
        value = requireText(value, "ownerId");
    }

    public static PlayerSleepOwnerId storyExecution(String value) {
        return new PlayerSleepOwnerId(value);
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return checked;
    }
}
