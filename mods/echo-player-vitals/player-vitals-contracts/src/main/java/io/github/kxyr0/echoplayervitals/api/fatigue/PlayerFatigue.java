package io.github.kxyr0.echoplayervitals.api.fatigue;

/**
 * Raw server-owned fatigue value. Zero means rested; maximum means exhausted.
 */
public record PlayerFatigue(int current, int maximum) {
    public PlayerFatigue {
        if (maximum <= 0) {
            throw new IllegalArgumentException("Maximum fatigue must be positive.");
        }
        if (current < 0 || current > maximum) {
            throw new IllegalArgumentException("Current fatigue must be between zero and maximum.");
        }
    }

    public static PlayerFatigue defaultState() {
        return new PlayerFatigue(0, 100);
    }

    public PlayerFatigueReserve reserve() {
        return new PlayerFatigueReserve(maximum - current, maximum);
    }

    public PlayerFatigue withCurrent(int nextCurrent) {
        return new PlayerFatigue(nextCurrent, maximum);
    }

    public PlayerFatigue withMaximum(int nextMaximum) {
        return new PlayerFatigue(Math.min(current, nextMaximum), nextMaximum);
    }
}
