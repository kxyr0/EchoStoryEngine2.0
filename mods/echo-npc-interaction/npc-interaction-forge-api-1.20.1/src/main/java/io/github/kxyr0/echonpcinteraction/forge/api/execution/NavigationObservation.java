package io.github.kxyr0.echonpcinteraction.forge.api.execution;

import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import java.util.Objects;

public record NavigationObservation(
        NavigationMoveId moveId,
        String status,
        boolean terminal,
        boolean arrived,
        String message) {
    public NavigationObservation {
        moveId = Objects.requireNonNull(moveId, "moveId");
        status = Objects.requireNonNull(status, "status");
        message = Objects.requireNonNull(message, "message");
    }
}
