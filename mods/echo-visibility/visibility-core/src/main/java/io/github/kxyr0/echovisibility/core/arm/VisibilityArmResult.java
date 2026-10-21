package io.github.kxyr0.echovisibility.core.arm;

import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnostic;
import java.util.Objects;
import java.util.Optional;

public record VisibilityArmResult(
        Status status,
        Optional<VisibilityArmSnapshot> arm,
        Optional<VisibilityDiagnostic> diagnostic) {
    public VisibilityArmResult {
        status = Objects.requireNonNull(status, "status");
        arm = Objects.requireNonNull(arm, "arm");
        diagnostic = Objects.requireNonNull(diagnostic, "diagnostic");
    }

    public enum Status {
        ARMED,
        ALREADY_ARMED,
        DISARMED,
        NOT_ARMED,
        LIMIT_REACHED
    }
}
