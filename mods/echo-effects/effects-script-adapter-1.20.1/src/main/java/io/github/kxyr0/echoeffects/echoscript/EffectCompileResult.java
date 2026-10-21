package io.github.kxyr0.echoeffects.echoscript;

import io.github.kxyr0.echoeffects.api.definition.EffectDefinition;
import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record EffectCompileResult(Optional<EffectDefinition> definition, List<EffectDiagnostic> diagnostics) {
    public EffectCompileResult {
        definition = Objects.requireNonNull(definition, "definition");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public boolean successful() {
        return definition.isPresent() && diagnostics.stream().noneMatch(EffectDiagnostic::isError);
    }
}
