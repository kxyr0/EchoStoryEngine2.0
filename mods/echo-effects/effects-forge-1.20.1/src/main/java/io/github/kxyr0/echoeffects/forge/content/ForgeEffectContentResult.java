package io.github.kxyr0.echoeffects.forge.content;

import io.github.kxyr0.echoeffects.api.definition.EffectDefinition;
import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnostic;
import java.util.List;
import java.util.Objects;

public record ForgeEffectContentResult(List<EffectDefinition> definitions, List<EffectDiagnostic> diagnostics) {
    public ForgeEffectContentResult {
        definitions = List.copyOf(Objects.requireNonNull(definitions, "definitions"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }
}
