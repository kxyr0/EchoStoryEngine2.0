package io.github.kxyr0.echonpcinteraction.api.result;

public enum NpcInteractionStatus {
    CREATED,
    WAITING_FOR_MATERIALIZATION,
    VALIDATING_TARGET,
    SELECTING_PROVIDER,
    RESOLVING_APPROACH,
    WAITING_FOR_NAVIGATION,
    ALIGNING,
    INTERACTING,
    VERIFYING,
    COMPLETED,
    CANCELLED,
    FAILED,
    UNCERTAIN,
    RECOVERING;

    public boolean terminal() {
        return this == COMPLETED || this == CANCELLED || this == FAILED || this == UNCERTAIN;
    }
}
