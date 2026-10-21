package io.github.kxyr0.echonavigation.api.movement;

public enum NavigationOutcome {
    ARRIVED,
    CANCELLED,
    PATH_NOT_FOUND,
    DESTINATION_INVALID,
    DESTINATION_UNLOADED,
    NPC_REMOVED,
    STUCK,
    BACKEND_UNAVAILABLE,
    TIMEOUT,
    OWNER_CANCELLED,
    INTERNAL_FAILURE
}
