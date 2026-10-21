package io.github.kxyr0.echodevscenarios.api;

import java.util.Objects;

public record ScenarioId(String value) implements Comparable<ScenarioId> {
    public ScenarioId {
        value = Objects.requireNonNull(value, "value").trim();
        if (!value.matches("[a-z0-9_]+(\\.[a-z0-9_]+)*")) {
            throw new IllegalArgumentException("Invalid scenario id: " + value);
        }
    }

    @Override
    public int compareTo(ScenarioId other) {
        return value.compareTo(other.value);
    }
}
