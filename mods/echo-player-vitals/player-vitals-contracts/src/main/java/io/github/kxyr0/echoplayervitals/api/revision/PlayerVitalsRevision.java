package io.github.kxyr0.echoplayervitals.api.revision;

/**
 * Monotonic revision for server-authoritative vitals snapshots.
 */
public record PlayerVitalsRevision(long value) implements Comparable<PlayerVitalsRevision> {
    public PlayerVitalsRevision {
        if (value < 0L) {
            throw new IllegalArgumentException("Vitals revision cannot be negative.");
        }
    }

    public static PlayerVitalsRevision initial() {
        return new PlayerVitalsRevision(1L);
    }

    public PlayerVitalsRevision next() {
        return new PlayerVitalsRevision(value + 1L);
    }

    @Override
    public int compareTo(PlayerVitalsRevision other) {
        return Long.compare(value, other.value);
    }
}
