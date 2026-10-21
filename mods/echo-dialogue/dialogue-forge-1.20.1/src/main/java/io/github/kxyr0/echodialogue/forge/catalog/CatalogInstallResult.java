package io.github.kxyr0.echodialogue.forge.catalog;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import java.util.List;
import java.util.Objects;

/**
 * Atomic catalog apply result.
 */
public record CatalogInstallResult(
        boolean installed,
        CompiledDialogueCatalogSnapshot visible,
        List<DialogueDiagnostic> diagnostics
) {
    public CatalogInstallResult {
        visible = Objects.requireNonNull(visible, "visible");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }
}
