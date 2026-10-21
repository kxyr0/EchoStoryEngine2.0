package io.github.kxyr0.echoeffects.api.time;

public record EffectDuration(long ticks) {
    private static final long INFINITE = -1L;

    public EffectDuration {
        if (ticks == 0L || ticks < INFINITE) {
            throw new IllegalArgumentException("Duration must be positive ticks or infinite.");
        }
    }

    public static EffectDuration ticks(long ticks) {
        return new EffectDuration(ticks);
    }

    public static EffectDuration seconds(double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0.0D) {
            throw new IllegalArgumentException("Seconds must be positive and finite.");
        }
        return ticks(Math.max(1L, Math.round(seconds * 20.0D)));
    }

    public static EffectDuration infinite() {
        return new EffectDuration(INFINITE);
    }

    public boolean infiniteDuration() {
        return ticks == INFINITE;
    }
}
