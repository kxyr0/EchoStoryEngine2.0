package io.github.kxyr0.echoplayersleep.api.id;

import java.util.Objects;
import java.util.regex.Pattern;

public record PlayerSleepScenarioId(String value) {
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public PlayerSleepScenarioId {
        value = requireId(value, "scenarioId");
    }

    public static PlayerSleepScenarioId parse(String value) {
        return new PlayerSleepScenarioId(value);
    }

    private static String requireId(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (!ID.matcher(checked).matches()) {
            throw new IllegalArgumentException(name + " must be a namespaced id");
        }
        return checked;
    }
}
