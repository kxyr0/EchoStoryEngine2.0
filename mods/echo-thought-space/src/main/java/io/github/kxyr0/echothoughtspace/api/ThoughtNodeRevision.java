package io.github.kxyr0.echothoughtspace.api;

public record ThoughtNodeRevision(long value) {
    public ThoughtNodeRevision {
        if (value < 0L) throw new IllegalArgumentException("Revision must not be negative.");
    }
}

