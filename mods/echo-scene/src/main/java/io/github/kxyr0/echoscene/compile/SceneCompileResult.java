package io.github.kxyr0.echoscene.compile;

import io.github.kxyr0.echoscene.api.definition.SceneDefinition;
import io.github.kxyr0.echoscene.api.descriptor.SceneDescriptor;
import io.github.kxyr0.echoscene.api.diagnostic.SceneDiagnostic;
import java.util.List;
import java.util.Optional;

public record SceneCompileResult(
        Optional<SceneDefinition> definition,
        Optional<SceneDescriptor> descriptor,
        List<SceneDiagnostic> diagnostics
) {
    public SceneCompileResult {
        definition = definition == null ? Optional.empty() : definition;
        descriptor = descriptor == null ? Optional.empty() : descriptor;
        diagnostics = List.copyOf(diagnostics);
    }

    public boolean successful() {
        return definition.isPresent() && diagnostics.stream()
                .noneMatch(value -> value.severity() == SceneDiagnostic.Severity.ERROR);
    }
}
