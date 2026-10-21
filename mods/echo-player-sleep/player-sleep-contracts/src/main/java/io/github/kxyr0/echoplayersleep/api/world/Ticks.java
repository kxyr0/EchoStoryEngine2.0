package io.github.kxyr0.echoplayersleep.api.world;

public record Ticks(long value) {
    public Ticks {
        if (value < 0L) {
            throw new IllegalArgumentException("ticks must not be negative");
        }
    }

    public static Ticks of(long value) {
        return new Ticks(value);
    }
}
