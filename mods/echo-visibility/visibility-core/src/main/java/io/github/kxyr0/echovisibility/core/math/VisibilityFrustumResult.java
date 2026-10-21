package io.github.kxyr0.echovisibility.core.math;

import io.github.kxyr0.echovisibility.api.geometry.VisibilityScreenRectangle;
import io.github.kxyr0.echovisibility.api.state.VisibilityFrustumClassification;
import java.util.Objects;
import java.util.Optional;

public record VisibilityFrustumResult(
        VisibilityFrustumClassification classification,
        Optional<VisibilityScreenRectangle> projectedBounds) {
    public VisibilityFrustumResult {
        classification = Objects.requireNonNull(classification, "classification");
        projectedBounds = Objects.requireNonNull(projectedBounds, "projectedBounds");
    }
}
