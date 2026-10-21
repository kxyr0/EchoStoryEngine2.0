package io.github.kxyr0.echotime.api.value;

public record EchoDay(long value) implements Comparable<EchoDay> {
    public static final EchoDay FIRST = new EchoDay(1L);

    public EchoDay {
        if (value < 1L) {
            throw new IllegalArgumentException("Echo day must be at least 1.");
        }
    }

    public EchoDay next() {
        return new EchoDay(Math.addExact(value, 1L));
    }

    @Override
    public int compareTo(EchoDay other) {
        return Long.compare(value, other.value);
    }
}
