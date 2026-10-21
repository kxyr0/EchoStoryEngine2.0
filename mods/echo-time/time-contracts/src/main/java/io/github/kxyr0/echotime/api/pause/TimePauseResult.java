package io.github.kxyr0.echotime.api.pause;

import io.github.kxyr0.echotime.api.identity.TimePauseOwner;
import java.util.Objects;

public record TimePauseResult(TimePauseStatus status, TimePauseOwner owner, int effectiveLeaseCount) {
    public TimePauseResult {
        status = Objects.requireNonNull(status, "status");
        owner = Objects.requireNonNull(owner, "owner");
        if (effectiveLeaseCount < 0) throw new IllegalArgumentException("Lease count cannot be negative.");
    }
}
