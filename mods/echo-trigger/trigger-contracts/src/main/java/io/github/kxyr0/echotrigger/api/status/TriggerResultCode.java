package io.github.kxyr0.echotrigger.api.status;

public enum TriggerResultCode {
    STARTED,
    EXISTING,
    COMPLETED,
    STOPPED,
    RESET,
    FIRED,
    NOT_FOUND,
    POLICY_DENIED,
    IDEMPOTENCY_CONFLICT,
    INVALID_DEFINITION
}
