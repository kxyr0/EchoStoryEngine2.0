package io.github.kxyr0.echovisibility.forge.api;

import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnostic;
import java.util.Objects;
import java.util.Optional;

public record VisibilityServiceResult(
        Status status,
        Optional<VisibilityWatchView> watch,
        Optional<VisibilityDiagnostic> diagnostic) {
    public VisibilityServiceResult {
        status = Objects.requireNonNull(status, "status");
        watch = Objects.requireNonNull(watch, "watch");
        diagnostic = Objects.requireNonNull(diagnostic, "diagnostic");
    }

    public boolean successful() {
        return status == Status.ARMED || status == Status.DISARMED
                || status == Status.ASSERTED || status == Status.CLAIMED
                || status == Status.COMMITTED || status == Status.REJECTED;
    }

    public enum Status {
        ARMED,
        DISARMED,
        ASSERTED,
        CLAIMED,
        COMMITTED,
        REJECTED,
        NOT_FOUND,
        NOT_ARMED,
        LIMIT_REACHED,
        BLIND_WINDOW_CLOSED,
        FAILED
    }
}
