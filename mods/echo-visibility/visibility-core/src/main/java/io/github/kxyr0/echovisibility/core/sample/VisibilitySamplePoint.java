package io.github.kxyr0.echovisibility.core.sample;

import io.github.kxyr0.echovisibility.api.geometry.VisibilityVector3;
import io.github.kxyr0.echovisibility.api.sample.VisibilitySampleKind;
import java.util.Objects;

public record VisibilitySamplePoint(
        VisibilitySampleKind kind,
        int ordinal,
        VisibilityVector3 position) {
    public VisibilitySamplePoint {
        kind = Objects.requireNonNull(kind, "kind");
        position = Objects.requireNonNull(position, "position");
        if (ordinal < 0) {
            throw new IllegalArgumentException("Sample ordinal must not be negative.");
        }
    }
}
