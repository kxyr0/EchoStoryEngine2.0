package io.github.kxyr0.echoplayerhud.api.gradient;

/**
 * One ARGB color stop in a stepped gradient.
 */
public record HudColorStep(double at, int argb) {
    public HudColorStep {
        if (!Double.isFinite(at) || at < 0.0D || at > 1.0D) {
            throw new IllegalArgumentException("Gradient step position must be finite in 0..1.");
        }
    }
}
