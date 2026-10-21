package io.github.kxyr0.echoplayervitals.api.fatigue;

/**
 * Display-facing reserve value, inverted from raw fatigue.
 */
public record PlayerFatigueReserve(int current, int maximum) {
    public PlayerFatigueReserve {
        if (maximum <= 0) {
            throw new IllegalArgumentException("Maximum fatigue reserve must be positive.");
        }
        if (current < 0 || current > maximum) {
            throw new IllegalArgumentException("Fatigue reserve must be between zero and maximum.");
        }
    }

    public double normalized() {
        return (double) current / (double) maximum;
    }
}
