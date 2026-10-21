package io.github.kxyr0.echoplayervitals.api.health;

/**
 * Immutable view of vanilla health. Vitals does not own combat authority.
 */
public record PlayerHealthSnapshot(float current, float maximum, boolean alive) {
    public PlayerHealthSnapshot {
        if (!Float.isFinite(current) || !Float.isFinite(maximum)) {
            throw new IllegalArgumentException("Health values must be finite.");
        }
        if (maximum <= 0.0F || current < 0.0F || current > maximum) {
            throw new IllegalArgumentException("Health current must be between zero and maximum.");
        }
    }
}
