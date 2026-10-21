package io.github.kxyr0.echoplayer.api.spark;

/**
 * Durable story mana values for one player.
 */
public record PlayerSpark(int current, int maximum) {
    public static final int HARD_MAXIMUM = 1_000_000;

    public PlayerSpark {
        if (maximum <= 0) {
            throw new IllegalArgumentException("Maximum Spark must be positive.");
        }
        if (maximum > HARD_MAXIMUM) {
            throw new IllegalArgumentException("Maximum Spark exceeds hard limit.");
        }
        if (current < 0 || current > maximum) {
            throw new IllegalArgumentException("Current Spark must be between zero and maximum.");
        }
    }

    public static PlayerSpark full(int maximum) {
        return new PlayerSpark(maximum, maximum);
    }
}
