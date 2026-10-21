package io.github.kxyr0.echoobservation.echoscript;

import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnostic;
import io.github.kxyr0.echoobservation.api.model.ObservationResourceDefinition;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ObservationCompileResult(
        Optional<ObservationResourceDefinition> resource,
        List<ObservationDiagnostic> diagnostics) {
    public ObservationCompileResult {
        resource = Objects.requireNonNull(resource, "resource");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public boolean successful() {
        return resource.isPresent() && diagnostics.stream().noneMatch(ObservationDiagnostic::error);
    }
}
