package io.github.kxyr0.echomagic.forge.content;

import io.github.kxyr0.echomagic.api.definition.SpellDefinition;
import io.github.kxyr0.echomagic.api.diagnostic.MagicDiagnostic;
import java.util.List;

public record ForgeSpellContentResult(List<SpellDefinition> definitions, List<MagicDiagnostic> diagnostics) {
    public ForgeSpellContentResult {
        definitions = List.copyOf(definitions);
        diagnostics = List.copyOf(diagnostics);
    }
}

