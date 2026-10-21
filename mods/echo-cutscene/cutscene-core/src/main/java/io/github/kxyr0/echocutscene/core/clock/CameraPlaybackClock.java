package io.github.kxyr0.echocutscene.core.clock;

public final class CameraPlaybackClock {
    private final long startedAtMillis;
    private final long durationMillis;
    private final double speed;

    public CameraPlaybackClock(long startedAtMillis, long durationMillis, double speed) {
        if (durationMillis < 0L || !Double.isFinite(speed) || speed <= 0.0D) {
            throw new IllegalArgumentException("Clock requires non-negative duration and positive speed.");
        }
        this.startedAtMillis = startedAtMillis;
        this.durationMillis = durationMillis;
        this.speed = speed;
    }

    public PlaybackTime at(long nowMillis) {
        long raw = Math.max(0L, nowMillis - startedAtMillis);
        long scaled = Math.min(durationMillis, Math.round(raw * speed));
        return new PlaybackTime(scaled, scaled >= durationMillis);
    }
}
