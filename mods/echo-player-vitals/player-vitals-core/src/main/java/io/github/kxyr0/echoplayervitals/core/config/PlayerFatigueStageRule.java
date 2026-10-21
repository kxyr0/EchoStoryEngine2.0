package io.github.kxyr0.echoplayervitals.core.config;

import io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigueStage;
import java.util.Objects;
import java.util.Optional;

/**
 * One reserve threshold rule from the fatigue configuration.
 */
public record PlayerFatigueStageRule(
        PlayerFatigueStage stage,
        Optional<Double> reserveAtMost,
        Optional<Double> reserveAbove,
        double movementMultiplier,
        double nauseaIntensity
) {
    public PlayerFatigueStageRule {
        stage = Objects.requireNonNull(stage, "stage");
        reserveAtMost = normalize(reserveAtMost, "reserveAtMost");
        reserveAbove = normalize(reserveAbove, "reserveAbove");
        if (!Double.isFinite(movementMultiplier) || movementMultiplier <= 0.0D) {
            throw new IllegalArgumentException("Movement multiplier must be positive and finite.");
        }
        if (!Double.isFinite(nauseaIntensity) || nauseaIntensity < 0.0D) {
            throw new IllegalArgumentException("Nausea intensity must be non-negative and finite.");
        }
    }

    public boolean matches(double normalizedReserve) {
        boolean belowCeiling = reserveAtMost.map(limit -> normalizedReserve <= limit).orElse(true);
        boolean aboveFloor = reserveAbove.map(limit -> normalizedReserve > limit).orElse(true);
        return belowCeiling && aboveFloor;
    }

    private static Optional<Double> normalize(Optional<Double> value, String name) {
        Optional<Double> normalized = Objects.requireNonNull(value, name);
        normalized.ifPresent(number -> {
            if (!Double.isFinite(number) || number < 0.0D || number > 1.0D) {
                throw new IllegalArgumentException(name + " must be finite in 0..1.");
            }
        });
        return normalized;
    }
}
