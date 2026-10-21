package io.github.kxyr0.echodevscenarios.compile;

import io.github.kxyr0.echodevscenarios.definition.ScenarioDefinition;
import io.github.kxyr0.echodevscenarios.definition.ScenarioDescriptor;
import java.util.List;
import java.util.Optional;

public record ScenarioCompileResult(
        Optional<ScenarioDefinition> definition,
        Optional<ScenarioDescriptor> descriptor,
        List<ScenarioDiagnostic> diagnostics
) {
    public ScenarioCompileResult { diagnostics = List.copyOf(diagnostics); }
    public boolean successful() { return definition.isPresent() && diagnostics.isEmpty(); }
}
