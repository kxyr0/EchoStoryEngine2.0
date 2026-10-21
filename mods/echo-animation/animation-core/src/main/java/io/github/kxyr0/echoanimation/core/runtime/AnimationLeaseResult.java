package io.github.kxyr0.echoanimation.core.runtime;

import java.util.Objects;
import java.util.Optional;

/**
 * Explicit result for channel ownership acquisition.
 */
public record AnimationLeaseResult(Status status, Optional<AnimationLease> lease) {
    public AnimationLeaseResult {
        status = Objects.requireNonNull(status, "status");
        lease = Objects.requireNonNull(lease, "lease");
    }

    public enum Status {
        ACQUIRED,
        CONFLICT,
        UNKNOWN_CHANNEL
    }
}
