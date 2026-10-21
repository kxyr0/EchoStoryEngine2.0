package io.github.kxyr0.echovisibility.forge.api;

import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnostic;
import io.github.kxyr0.echovisibility.api.target.VisibilityTargetBounds;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

public record VisibilityTargetResolution(
        Status status,
        Optional<VisibilityTargetBounds> bounds,
        OptionalInt clientEntityId,
        Optional<VisibilityDiagnostic> diagnostic) {
    public VisibilityTargetResolution {
        status = Objects.requireNonNull(status, "status");
        bounds = Objects.requireNonNull(bounds, "bounds");
        clientEntityId = Objects.requireNonNull(clientEntityId, "clientEntityId");
        diagnostic = Objects.requireNonNull(diagnostic, "diagnostic");
    }

    public enum Status {
        RESOLVED,
        TARGET_NOT_FOUND,
        TARGET_NOT_MATERIALIZED,
        DIMENSION_MISMATCH,
        INVALID
    }
}
