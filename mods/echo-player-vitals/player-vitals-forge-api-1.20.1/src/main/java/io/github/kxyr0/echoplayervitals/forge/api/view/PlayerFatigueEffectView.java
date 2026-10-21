package io.github.kxyr0.echoplayervitals.forge.api.view;

import io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigueStage;
import java.util.Objects;

/**
 * Read-only fatigue gameplay effect projection.
 */
public record PlayerFatigueEffectView(
        PlayerFatigueStage stage,
        double movementMultiplier,
        double nauseaIntensity
) {
    public PlayerFatigueEffectView {
        stage = Objects.requireNonNull(stage, "stage");
        if (!Double.isFinite(movementMultiplier) || movementMultiplier <= 0.0D) {
            throw new IllegalArgumentException("Movement multiplier must be positive and finite.");
        }
        if (!Double.isFinite(nauseaIntensity) || nauseaIntensity < 0.0D) {
            throw new IllegalArgumentException("Nausea intensity must be non-negative and finite.");
        }
    }
}
