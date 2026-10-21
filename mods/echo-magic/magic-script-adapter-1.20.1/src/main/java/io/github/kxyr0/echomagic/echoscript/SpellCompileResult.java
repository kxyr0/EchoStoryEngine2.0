package io.github.kxyr0.echomagic.echoscript;

import io.github.kxyr0.echomagic.api.definition.SpellDefinition;
import io.github.kxyr0.echomagic.api.diagnostic.MagicDiagnostic;
import java.util.List;
import java.util.Optional;

public record SpellCompileResult(Optional<SpellDefinition> definition, List<MagicDiagnostic> diagnostics) {
    public SpellCompileResult {
        definition = definition == null ? Optional.empty() : definition;
        diagnostics = List.copyOf(diagnostics);
    }
}

