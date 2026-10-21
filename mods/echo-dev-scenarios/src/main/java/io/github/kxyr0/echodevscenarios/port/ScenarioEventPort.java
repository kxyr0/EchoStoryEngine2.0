package io.github.kxyr0.echodevscenarios.port;

import io.github.kxyr0.echodevscenarios.api.ScenarioInspection;

@FunctionalInterface
public interface ScenarioEventPort {
    void changed(ScenarioInspection inspection);

    static ScenarioEventPort quiet() { return ignored -> { }; }
}
