package io.github.kxyr0.echoeffects.core.contribution;

public record EffectContributionSnapshot(
        double movementSpeedMultiplier,
        double fovOffset,
        double cameraRoll,
        double cameraShake,
        double screenTintOpacity,
        double screenVignetteOpacity
) {
    public EffectContributionSnapshot {
        if (!Double.isFinite(movementSpeedMultiplier) || movementSpeedMultiplier <= 0.0D) {
            throw new IllegalArgumentException("Movement multiplier must be positive and finite.");
        }
    }

    public static EffectContributionSnapshot neutral() {
        return new EffectContributionSnapshot(1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    }
}
