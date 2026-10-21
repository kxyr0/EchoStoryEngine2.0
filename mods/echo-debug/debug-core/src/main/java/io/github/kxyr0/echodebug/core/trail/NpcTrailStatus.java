package io.github.kxyr0.echodebug.core.trail;

public enum NpcTrailStatus {
    ACTIVE,
    ARRIVED,
    CANCELLED,
    FAILED;

    public boolean terminal() {
        return this != ACTIVE;
    }
}
