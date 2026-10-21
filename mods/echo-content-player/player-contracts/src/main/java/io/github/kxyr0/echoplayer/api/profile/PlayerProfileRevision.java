package io.github.kxyr0.echoplayer.api.profile;

/**
 * Monotonic profile revision.
 */
public record PlayerProfileRevision(long value) implements Comparable<PlayerProfileRevision> {
    public PlayerProfileRevision {
        if (value < 0L) {
            throw new IllegalArgumentException("Profile revision must not be negative.");
        }
    }

    public static PlayerProfileRevision initial() {
        return new PlayerProfileRevision(1L);
    }

    public PlayerProfileRevision next() {
        return new PlayerProfileRevision(value + 1L);
    }

    @Override
    public int compareTo(PlayerProfileRevision other) {
        return Long.compare(value, other.value);
    }
}
