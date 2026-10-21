package io.github.kxyr0.echoeffects.api.curve;

import java.util.Objects;

public record EffectCurveKey(long tick, double value, EffectInterpolation interpolation) {
    public EffectCurveKey {
        if (tick < 0L) {
            throw new IllegalArgumentException("Curve key tick must not be negative.");
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Curve key value must be finite.");
        }
        interpolation = Objects.requireNonNull(interpolation, "interpolation");
    }
}
