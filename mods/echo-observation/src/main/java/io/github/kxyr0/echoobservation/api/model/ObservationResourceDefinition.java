package io.github.kxyr0.echoobservation.api.model;

import io.github.kxyr0.echoobservation.api.identity.ObservationResourceId;
import java.util.List;
import java.util.Objects;

public record ObservationResourceDefinition(
        ObservationResourceId id,
        List<ObservationDefinition> observations,
        long revision,
        String sourceId) {
    public ObservationResourceDefinition {
        id = Objects.requireNonNull(id, "id");
        observations = List.copyOf(Objects.requireNonNull(observations, "observations"));
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        if (observations.isEmpty() || revision < 0L || sourceId.isBlank()) {
            throw new IllegalArgumentException("Invalid Observation resource definition.");
        }
    }
}
