package io.github.kxyr0.echodevscenarios.port;

import io.github.kxyr0.echodevscenarios.api.ScenarioId;
import java.util.Optional;

@FunctionalInterface
public interface ScenarioValidationGate {
    Optional<String> firstError(ScenarioId scenario);
}
