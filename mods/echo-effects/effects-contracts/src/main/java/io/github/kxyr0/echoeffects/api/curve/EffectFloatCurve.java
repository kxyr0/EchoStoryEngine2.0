package io.github.kxyr0.echoeffects.api.curve;

import java.util.List;
import java.util.Objects;

public record EffectFloatCurve(List<EffectCurveKey> keys) {
    public EffectFloatCurve {
        keys = List.copyOf(Objects.requireNonNull(keys, "keys"));
        if (keys.isEmpty()) {
            throw new IllegalArgumentException("Curve must contain at least one key.");
        }
    }

    public static EffectFloatCurve constant(double value) {
        return new EffectFloatCurve(List.of(new EffectCurveKey(0L, value, EffectInterpolation.STEP)));
    }
}
