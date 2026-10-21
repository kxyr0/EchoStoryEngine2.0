package io.github.kxyr0.echoscene.compile.binding;

import io.github.kxyr0.echoscene.api.definition.SceneDefinition;
import io.github.kxyr0.echoscene.api.descriptor.SceneDescriptor;
import io.github.kxyr0.echoscene.api.diagnostic.SceneDiagnostic;
import java.util.List;
import java.util.Optional;

public record BindingResult(
        Optional<SceneDefinition> definition,
        Optional<SceneDescriptor> descriptor,
        List<SceneDiagnostic> diagnostics
) {
    public BindingResult {
        diagnostics = List.copyOf(diagnostics);
    }
}
