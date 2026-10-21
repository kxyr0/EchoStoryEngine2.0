package io.github.kxyr0.echoplayervitals.core.mutation;

import io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigue;
import java.util.Objects;

/**
 * Immutable fatigue mutation outcome.
 */
public record PlayerFatigueMutationResult(PlayerFatigue fatigue, boolean changed) {
    public PlayerFatigueMutationResult {
        fatigue = Objects.requireNonNull(fatigue, "fatigue");
    }
}
