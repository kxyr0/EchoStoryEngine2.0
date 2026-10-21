package io.github.kxyr0.echoseating.api.id;

public record SeatingRevision(long value) {
    public SeatingRevision {
        if (value < 0L) {
            throw new IllegalArgumentException("SeatingRevision must not be negative.");
        }
    }

    public static SeatingRevision initial() {
        return new SeatingRevision(1L);
    }

    public SeatingRevision next() {
        return new SeatingRevision(value + 1L);
    }
}
