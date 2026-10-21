package io.github.kxyr0.echovisibility.api.debug;

import io.github.kxyr0.echovisibility.api.geometry.VisibilityVector3;
import io.github.kxyr0.echovisibility.api.sample.VisibilitySampleKind;
import java.util.Objects;
import java.util.Optional;

public record VisibilityDebugSample(
        VisibilitySampleKind kind,
        VisibilityVector3 position,
        boolean inViewport,
        String rayStatus,
        Optional<VisibilityVector3> hitPosition,
        Optional<String> occluder) {
    public VisibilityDebugSample {
        kind = Objects.requireNonNull(kind, "kind");
        position = Objects.requireNonNull(position, "position");
        rayStatus = Objects.requireNonNull(rayStatus, "rayStatus");
        hitPosition = Objects.requireNonNull(hitPosition, "hitPosition");
        occluder = Objects.requireNonNull(occluder, "occluder");
    }
}
