package io.github.kxyr0.echoobservation.api.action;

import io.github.kxyr0.echoobservation.api.identity.ObservationCompletionId;
import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import java.util.Objects;
import java.util.UUID;

public record ObservationActionContext(
        UUID playerId,
        ObservationId observationId,
        ObservationCompletionId completionId,
        ObservationActionExecutionId executionId) {
    public ObservationActionContext {
        playerId = Objects.requireNonNull(playerId, "playerId");
        observationId = Objects.requireNonNull(observationId, "observationId");
        completionId = Objects.requireNonNull(completionId, "completionId");
        executionId = Objects.requireNonNull(executionId, "executionId");
    }
}
