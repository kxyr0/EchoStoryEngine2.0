package io.github.kxyr0.echotrigger.core.definition;

import io.github.kxyr0.echotrigger.api.model.TriggerDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record TriggerCompileResult(
        Optional<CompiledTriggerDefinition> definition,
        List<TriggerDiagnostic> diagnostics
) {
    public TriggerCompileResult {
        definition = Objects.requireNonNull(definition, "definition");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public boolean successful() {
        return definition.isPresent() && diagnostics.stream().noneMatch(TriggerDiagnostic::isError);
    }
}
