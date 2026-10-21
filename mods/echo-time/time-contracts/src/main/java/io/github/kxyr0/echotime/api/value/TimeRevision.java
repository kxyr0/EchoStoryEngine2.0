package io.github.kxyr0.echotime.api.value;

public record TimeRevision(long value) implements Comparable<TimeRevision> {
    public static final TimeRevision INITIAL = new TimeRevision(1L);

    public TimeRevision {
        if (value < 1L) {
            throw new IllegalArgumentException("Time revision must be positive.");
        }
    }

    public TimeRevision next() {
        return new TimeRevision(Math.addExact(value, 1L));
    }

    @Override
    public int compareTo(TimeRevision other) {
        return Long.compare(value, other.value);
    }
}
