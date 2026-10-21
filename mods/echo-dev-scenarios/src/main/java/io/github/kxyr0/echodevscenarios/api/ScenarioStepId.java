package io.github.kxyr0.echodevscenarios.api;

public record ScenarioStepId(int value) {
    public ScenarioStepId {
        if (value < 0) throw new IllegalArgumentException("Scenario step id must not be negative.");
    }
}
