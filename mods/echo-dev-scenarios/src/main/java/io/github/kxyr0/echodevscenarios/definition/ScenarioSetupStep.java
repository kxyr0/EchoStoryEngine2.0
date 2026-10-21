package io.github.kxyr0.echodevscenarios.definition;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echodevscenarios.api.ScenarioStepId;
import java.util.List;
import java.util.Objects;

public record ScenarioSetupStep(
        ScenarioStepId id,
        String service,
        String operation,
        List<String> arguments,
        boolean blocking,
        SourceSpan span
) {
    public ScenarioSetupStep {
        id = Objects.requireNonNull(id, "id");
        service = Objects.requireNonNull(service, "service");
        operation = Objects.requireNonNull(operation, "operation");
        arguments = List.copyOf(arguments);
        span = Objects.requireNonNull(span, "span");
    }
}
