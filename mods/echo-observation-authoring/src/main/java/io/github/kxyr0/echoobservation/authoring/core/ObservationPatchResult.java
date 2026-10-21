package io.github.kxyr0.echoobservation.authoring.core;

import java.util.Objects;
import java.util.Optional;

public record ObservationPatchResult(Optional<String> candidate, String message) {
    public ObservationPatchResult {
        candidate = Objects.requireNonNull(candidate, "candidate");
        message = Objects.requireNonNull(message, "message");
    }

    public boolean successful() { return candidate.isPresent(); }
}
