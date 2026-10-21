package io.github.kxyr0.echonpcinteraction.forge.api.view;

import io.github.kxyr0.echonpcinteraction.api.diagnostic.NpcInteractionDiagnostic;
import java.util.List;
import java.util.Objects;

public record NpcInteractionDiagnosticsView(long revision, List<NpcInteractionDiagnostic> diagnostics) {
    public NpcInteractionDiagnosticsView {
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        if (revision < 0L) {
            throw new IllegalArgumentException("revision must not be negative.");
        }
    }
}
