package io.github.kxyr0.echotrigger.api.authoring.catalog;

import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record TriggerAreaDefinitionView(
        TriggerId triggerId,
        String areaName,
        String dimension,
        TriggerRevision definitionRevision,
        AreaGeometrySnapshot geometry,
        Optional<AreaSourceLocation> sourceLocation,
        List<AreaDiagnosticView> diagnostics
) {
    public TriggerAreaDefinitionView {
        triggerId = Objects.requireNonNull(triggerId, "triggerId");
        areaName = Objects.requireNonNull(areaName, "areaName").trim();
        dimension = Objects.requireNonNull(dimension, "dimension").trim();
        definitionRevision = Objects.requireNonNull(definitionRevision, "definitionRevision");
        geometry = Objects.requireNonNull(geometry, "geometry");
        sourceLocation = Objects.requireNonNull(sourceLocation, "sourceLocation");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        if (areaName.isEmpty() || dimension.isEmpty()) {
            throw new IllegalArgumentException("Area name and dimension are required.");
        }
    }
}
