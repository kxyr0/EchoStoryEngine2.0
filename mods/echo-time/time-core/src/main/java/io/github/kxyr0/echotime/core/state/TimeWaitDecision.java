package io.github.kxyr0.echotime.core.state;

import java.util.Objects;

public record TimeWaitDecision(Status status, TimeWait entry) {
    public TimeWaitDecision {
        status = Objects.requireNonNull(status, "status");
        entry = Objects.requireNonNull(entry, "entry");
    }

    public enum Status {
        WAITING,
        COMPLETE,
        IDEMPOTENCY_CONFLICT
    }
}
