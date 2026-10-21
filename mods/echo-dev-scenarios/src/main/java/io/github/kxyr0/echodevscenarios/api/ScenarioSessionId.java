package io.github.kxyr0.echodevscenarios.api;

import java.util.Objects;

public record ScenarioSessionId(String value) {
    public ScenarioSessionId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Scenario session id must not be empty.");
    }
}
