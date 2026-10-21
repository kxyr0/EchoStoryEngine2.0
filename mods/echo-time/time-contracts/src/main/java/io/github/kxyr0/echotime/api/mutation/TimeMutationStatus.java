package io.github.kxyr0.echotime.api.mutation;

public enum TimeMutationStatus {
    APPLIED,
    DUPLICATE,
    IDEMPOTENCY_CONFLICT,
    CROSSING_LIMIT_EXCEEDED,
    OVERFLOW,
    REJECTED
}
