package io.github.kxyr0.echonavigation.core.movement;

import io.github.kxyr0.echonavigation.api.movement.NavigationFailure;
import io.github.kxyr0.echonavigation.api.movement.NavigationSnapshot;
import java.util.Objects;
import java.util.Optional;

public record MovementStartResult(Status status, Optional<NavigationSnapshot> snapshot,
        Optional<NavigationFailure> failure) {
    public MovementStartResult {
        status = Objects.requireNonNull(status, "status");
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        failure = Objects.requireNonNull(failure, "failure");
    }

    public enum Status {
        ACCEPTED,
        DUPLICATE_COMMAND,
        COMMAND_CONFLICT,
        SUBJECT_BUSY,
        INVALID
    }
}
