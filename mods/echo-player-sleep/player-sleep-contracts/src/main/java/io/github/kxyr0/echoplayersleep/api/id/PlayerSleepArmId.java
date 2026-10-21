package io.github.kxyr0.echoplayersleep.api.id;

import java.util.Objects;
import java.util.UUID;

public record PlayerSleepArmId(String value) {
    public PlayerSleepArmId {
        value = requireText(value, "armId");
    }

    public static PlayerSleepArmId random() {
        return new PlayerSleepArmId(UUID.randomUUID().toString());
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return checked;
    }
}
