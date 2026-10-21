package io.github.kxyr0.echonavigation.forge.api.agent;

import java.util.Objects;
import java.util.Optional;

public record NavigationAgentResolution(Status status, Optional<NavigationAgentHandle> handle, String message) {
    public NavigationAgentResolution {
        status = Objects.requireNonNull(status, "status");
        handle = Objects.requireNonNull(handle, "handle");
        message = Objects.requireNonNull(message, "message");
    }

    public static NavigationAgentResolution resolved(NavigationAgentHandle handle) {
        return new NavigationAgentResolution(Status.RESOLVED, Optional.of(handle), "Navigation agent resolved.");
    }

    public static NavigationAgentResolution missing(Status status, String message) {
        return new NavigationAgentResolution(status, Optional.empty(), message);
    }

    public enum Status {
        RESOLVED,
        SUBJECT_NOT_FOUND,
        NOT_MATERIALIZED,
        WRONG_DIMENSION,
        REMOVED,
        UNAVAILABLE
    }
}
