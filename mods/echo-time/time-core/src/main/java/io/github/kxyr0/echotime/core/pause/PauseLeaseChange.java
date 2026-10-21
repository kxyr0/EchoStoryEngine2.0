package io.github.kxyr0.echotime.core.pause;

import io.github.kxyr0.echotime.api.identity.TimePauseLease;
import java.util.Objects;
import java.util.Optional;

public record PauseLeaseChange(Status status, Optional<TimePauseLease> lease) {
    public PauseLeaseChange {
        status = Objects.requireNonNull(status, "status");
        lease = Objects.requireNonNull(lease, "lease");
    }

    public enum Status {
        ACQUIRED,
        ALREADY_OWNED,
        RELEASED,
        NOT_OWNED
    }
}
