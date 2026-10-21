package io.github.kxyr0.echoobservation.api.action;

import io.github.kxyr0.echoobservation.api.identity.ObservationCompletionId;
import java.util.Objects;

public record ObservationActionExecutionId(String value) {
    public ObservationActionExecutionId {
        value = Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Observation action execution ID must not be blank.");
        }
    }

    public static ObservationActionExecutionId of(ObservationCompletionId completionId, int actionIndex) {
        return new ObservationActionExecutionId(completionId.value() + "/action/" + actionIndex);
    }
}
