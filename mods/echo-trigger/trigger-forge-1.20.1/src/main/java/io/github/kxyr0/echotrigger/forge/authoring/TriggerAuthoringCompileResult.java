package io.github.kxyr0.echotrigger.forge.authoring;

import io.github.kxyr0.echotrigger.api.authoring.catalog.AreaDiagnosticView;
import io.github.kxyr0.echotrigger.core.definition.CompiledTriggerDefinition;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

record TriggerAuthoringCompileResult(
        Optional<CompiledTriggerDefinition> definition,
        List<AreaDiagnosticView> diagnostics,
        String fingerprint,
        int parserCalls,
        int binderCalls
) {
    TriggerAuthoringCompileResult {
        definition = Objects.requireNonNull(definition, "definition");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
    }

    boolean successful() {
        return definition.isPresent() && diagnostics.isEmpty();
    }
}
