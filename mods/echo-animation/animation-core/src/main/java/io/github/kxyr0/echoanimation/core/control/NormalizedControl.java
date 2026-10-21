package io.github.kxyr0.echoanimation.core.control;

import io.github.kxyr0.echoanimation.api.id.FaceControlId;
import java.util.Objects;

/**
 * Clamped normalized control value.
 */
public record NormalizedControl(FaceControlId id, double value) {
    public NormalizedControl {
        id = Objects.requireNonNull(id, "id");
        if (!Double.isFinite(value) || value < 0.0D || value > 1.0D) {
            throw new IllegalArgumentException("Normalized control value должен быть в диапазоне 0..1.");
        }
    }
}

