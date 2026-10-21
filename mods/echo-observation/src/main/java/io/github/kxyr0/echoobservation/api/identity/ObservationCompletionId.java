package io.github.kxyr0.echoobservation.api.identity;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public record ObservationCompletionId(String value) {
    public ObservationCompletionId {
        value = Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Observation completion ID must not be blank.");
        }
    }

    public static ObservationCompletionId deterministic(UUID playerId, ObservationId observationId) {
        String seed = playerId + "/" + observationId;
        return new ObservationCompletionId(UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString());
    }
}
