package io.github.kxyr0.echoplayersleep.api.id;

import java.util.Objects;

public record PlayerSleepCommandId(String value) {
    public PlayerSleepCommandId {
        value = requireText(value, "commandId");
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return checked;
    }
}
