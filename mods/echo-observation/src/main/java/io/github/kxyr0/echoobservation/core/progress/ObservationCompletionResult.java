package io.github.kxyr0.echoobservation.core.progress;

import io.github.kxyr0.echoobservation.api.identity.ObservationCompletionId;
import java.util.Objects;
import java.util.Optional;

public record ObservationCompletionResult(Status status, Optional<ObservationCompletionId> completionId) {
    public enum Status { COMPLETED, ALREADY_COMPLETED, ACTION_UNAVAILABLE, ACTION_FAILED }

    public ObservationCompletionResult {
        status = Objects.requireNonNull(status, "status");
        completionId = Objects.requireNonNull(completionId, "completionId");
    }
}
