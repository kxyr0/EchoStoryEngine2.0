package io.github.kxyr0.echonpcinteraction.forge.api.execution;

import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import java.util.Objects;
import java.util.Optional;

public record NavigationDispatch(Status status, Optional<NavigationMoveId> moveId, String message) {
    public NavigationDispatch {
        status = Objects.requireNonNull(status, "status");
        moveId = Objects.requireNonNull(moveId, "moveId");
        message = Objects.requireNonNull(message, "message");
    }

    public enum Status {
        ACCEPTED,
        DUPLICATE,
        REJECTED,
        UNAVAILABLE
    }
}
