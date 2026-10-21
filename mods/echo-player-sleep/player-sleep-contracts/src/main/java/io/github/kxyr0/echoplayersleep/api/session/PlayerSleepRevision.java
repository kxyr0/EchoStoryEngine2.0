package io.github.kxyr0.echoplayersleep.api.session;

public record PlayerSleepRevision(long value) {
    public PlayerSleepRevision {
        if (value < 0L) {
            throw new IllegalArgumentException("revision must not be negative");
        }
    }

    public static PlayerSleepRevision initial() {
        return new PlayerSleepRevision(0L);
    }

    public PlayerSleepRevision next() {
        return new PlayerSleepRevision(value + 1L);
    }
}
