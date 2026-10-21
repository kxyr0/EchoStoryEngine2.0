package io.github.kxyr0.echoseating.core.binding;

import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnostic;
import io.github.kxyr0.echoseating.api.zone.SeatZoneDefinition;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record SeatZoneCompileResult(
        Optional<SeatZoneDefinition> definition,
        List<SeatingDiagnostic> diagnostics) {
    public SeatZoneCompileResult {
        definition = Objects.requireNonNull(definition, "definition");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public boolean successful() {
        return definition.isPresent() && diagnostics.isEmpty();
    }
}
