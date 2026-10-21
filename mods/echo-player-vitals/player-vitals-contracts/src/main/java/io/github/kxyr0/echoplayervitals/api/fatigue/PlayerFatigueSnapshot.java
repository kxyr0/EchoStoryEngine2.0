package io.github.kxyr0.echoplayervitals.api.fatigue;

import io.github.kxyr0.echoplayervitals.api.revision.PlayerVitalsRevision;
import java.util.Objects;

/**
 * Immutable fatigue projection used by public APIs and synchronization.
 */
public record PlayerFatigueSnapshot(
        PlayerFatigue fatigue,
        PlayerFatigueReserve reserve,
        PlayerFatigueStage stage,
        double movementMultiplier,
        double nauseaIntensity,
        PlayerVitalsRevision revision
) {
    public PlayerFatigueSnapshot {
        fatigue = Objects.requireNonNull(fatigue, "fatigue");
        reserve = Objects.requireNonNull(reserve, "reserve");
        stage = Objects.requireNonNull(stage, "stage");
        revision = Objects.requireNonNull(revision, "revision");
        if (!Double.isFinite(movementMultiplier) || movementMultiplier <= 0.0D) {
            throw new IllegalArgumentException("Movement multiplier must be a positive finite number.");
        }
        if (!Double.isFinite(nauseaIntensity) || nauseaIntensity < 0.0D) {
            throw new IllegalArgumentException("Nausea intensity must be a non-negative finite number.");
        }
    }
}
