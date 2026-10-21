package io.github.kxyr0.echoanimation.core.pose;

import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.model.InterpolationKind;
import java.util.Objects;

/**
 * Immutable transform keyframe.
 */
public record Keyframe(double timeSeconds, Transform3 transform, InterpolationKind interpolation) {
    public Keyframe {
        if (!Double.isFinite(timeSeconds) || timeSeconds < 0.0D) {
            throw new IllegalArgumentException("Keyframe timeSeconds должен быть finite и неотрицательным.");
        }
        transform = Objects.requireNonNull(transform, "transform");
        interpolation = Objects.requireNonNull(interpolation, "interpolation");
    }
}

