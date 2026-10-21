package io.github.kxyr0.echonavigation.api.movement;

public enum NavigationStatus {
    CREATED,
    WAITING_FOR_MATERIALIZATION,
    VALIDATING,
    PLANNING,
    MOVING,
    REPLANNING,
    SUSPENDED_UNLOADED,
    ARRIVED,
    CANCELLED,
    BLOCKED,
    FAILED;

    public boolean terminal() {
        return this == ARRIVED || this == CANCELLED || this == BLOCKED || this == FAILED;
    }
}
