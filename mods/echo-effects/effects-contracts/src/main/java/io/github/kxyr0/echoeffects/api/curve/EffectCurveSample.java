package io.github.kxyr0.echoeffects.api.curve;

public record EffectCurveSample(double value) {
    public EffectCurveSample {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Curve sample must be finite.");
        }
    }
}
