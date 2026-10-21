package io.github.kxyr0.echoplayervitals.core.mutation;

import io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigue;
import java.util.Objects;

/**
 * Atomic fatigue value transformations.
 */
public final class PlayerFatigueMutation {
    public PlayerFatigueMutationResult set(PlayerFatigue fatigue, int current) {
        Objects.requireNonNull(fatigue, "fatigue");
        int clamped = clamp(current, 0, fatigue.maximum());
        return result(fatigue, new PlayerFatigue(clamped, fatigue.maximum()));
    }

    public PlayerFatigueMutationResult add(PlayerFatigue fatigue, int amount) {
        Objects.requireNonNull(fatigue, "fatigue");
        if (amount < 0) {
            return result(fatigue, fatigue);
        }
        long next = (long) fatigue.current() + (long) amount;
        return result(fatigue, new PlayerFatigue((int) Math.min(next, fatigue.maximum()), fatigue.maximum()));
    }

    public PlayerFatigueMutationResult reduce(PlayerFatigue fatigue, int amount) {
        Objects.requireNonNull(fatigue, "fatigue");
        if (amount < 0) {
            return result(fatigue, fatigue);
        }
        return result(fatigue, new PlayerFatigue(Math.max(0, fatigue.current() - amount), fatigue.maximum()));
    }

    public PlayerFatigueMutationResult restoreFully(PlayerFatigue fatigue) {
        Objects.requireNonNull(fatigue, "fatigue");
        return result(fatigue, new PlayerFatigue(0, fatigue.maximum()));
    }

    private PlayerFatigueMutationResult result(PlayerFatigue before, PlayerFatigue after) {
        return new PlayerFatigueMutationResult(after, !before.equals(after));
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
