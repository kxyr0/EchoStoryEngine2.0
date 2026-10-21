package io.github.kxyr0.echoobservation.core.catalog;

import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnostic;
import io.github.kxyr0.echoobservation.api.model.ObservationResourceDefinition;
import java.util.List;
import java.util.Objects;

public record ObservationCatalogCandidate(
        List<ObservationResourceDefinition> resources,
        List<ObservationDiagnostic> diagnostics) {
    public ObservationCatalogCandidate {
        resources = List.copyOf(Objects.requireNonNull(resources, "resources"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public boolean valid() {
        return diagnostics.stream().noneMatch(ObservationDiagnostic::error);
    }
}
