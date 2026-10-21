package io.github.kxyr0.echovisibility.core.occlusion;

import io.github.kxyr0.echovisibility.api.geometry.VisibilityVector3;
import java.util.Objects;
import java.util.Optional;

public record VisibilityOcclusionResult(
        Status status,
        Optional<VisibilityVector3> hitPosition,
        Optional<String> occluder) {
    public VisibilityOcclusionResult {
        status = Objects.requireNonNull(status, "status");
        hitPosition = Objects.requireNonNull(hitPosition, "hitPosition");
        occluder = Objects.requireNonNull(occluder, "occluder");
        if (status == Status.BLOCKED && (hitPosition.isEmpty() || occluder.isEmpty())) {
            throw new IllegalArgumentException("Blocked ray requires hit position and occluder.");
        }
    }

    public static VisibilityOcclusionResult clear() {
        return new VisibilityOcclusionResult(Status.CLEAR, Optional.empty(), Optional.empty());
    }

    public static VisibilityOcclusionResult uncertain() {
        return new VisibilityOcclusionResult(Status.UNCERTAIN, Optional.empty(), Optional.empty());
    }

    public enum Status {
        CLEAR,
        BLOCKED,
        UNCERTAIN
    }
}
