package io.github.kxyr0.echonpc.echoscript;

import io.github.kxyr0.echonpc.api.form.definition.NpcFormDefinition;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of syntax parsing plus NpcFormResource semantic binding.
 */
public record NpcFormCompileResult(Optional<NpcFormDefinition> form, List<NpcDiagnostic> diagnostics) {
    public NpcFormCompileResult {
        form = Objects.requireNonNull(form, "form");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        if (form.isPresent() && diagnostics.stream().anyMatch(NpcDiagnostic::error)) {
            throw new IllegalArgumentException("Invalid form source must not produce a definition.");
        }
    }
}
