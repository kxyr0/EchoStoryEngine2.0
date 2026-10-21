package io.github.kxyr0.echocutscene.core.clock;

public record PlaybackTime(long elapsedMillis, boolean ended) {
    public PlaybackTime {
        if (elapsedMillis < 0L) {
            throw new IllegalArgumentException("elapsedMillis must not be negative.");
        }
    }
}
