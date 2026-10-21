package io.github.kxyr0.echoplayersleep.api.visual;

import java.util.Objects;

public record PlayerSleepVisualProfileId(String value) {
    public static final PlayerSleepVisualProfileId EYELIDS = new PlayerSleepVisualProfileId("EYELIDS");

    public PlayerSleepVisualProfileId {
        String checked = Objects.requireNonNull(value, "value").trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException("visual profile must not be blank");
        }
        value = checked;
    }
}
