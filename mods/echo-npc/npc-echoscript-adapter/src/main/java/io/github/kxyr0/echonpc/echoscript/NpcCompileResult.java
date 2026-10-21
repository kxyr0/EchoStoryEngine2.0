package io.github.kxyr0.echonpc.echoscript;

import io.github.kxyr0.echonpc.core.definition.NpcDefinition;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of syntax parsing plus NpcResource semantic binding.
 */
public record NpcCompileResult(Optional<NpcDefinition> definition, List<NpcDiagnostic> diagnostics) {
    public NpcCompileResult {
        definition = Objects.requireNonNull(definition, "definition");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        if (definition.isPresent() && diagnostics.stream().anyMatch(NpcDiagnostic::error)) {
            throw new IllegalArgumentException("Invalid source with ERROR diagnostics must not produce a definition.");
        }
    }
}
