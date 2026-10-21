package io.github.kxyr0.echoobservation.forge.client.presentation;

import io.github.kxyr0.echoobservation.api.model.ObservationDefinition;
import io.github.kxyr0.echoobservation.forge.client.projection.ObservationScreenPoint;
import java.util.Objects;

public record ProjectedObservation(
        ObservationDefinition definition,
        ObservationScreenPoint point,
        double physicalRadius,
        double centerDistance,
        double opacity) {
    public ProjectedObservation {
        definition = Objects.requireNonNull(definition, "definition");
        point = Objects.requireNonNull(point, "point");
        if (!Double.isFinite(opacity) || opacity < 0.0D || opacity > 1.0D) {
            throw new IllegalArgumentException("Opacity must be between zero and one.");
        }
    }
}
