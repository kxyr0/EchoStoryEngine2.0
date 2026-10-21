package io.github.kxyr0.echothoughtspace.api;

import java.util.Objects;

public record ThoughtNodeId(String value) implements Comparable<ThoughtNodeId> {
    public ThoughtNodeId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Thought node ID must not be blank.");
    }

    @Override public int compareTo(ThoughtNodeId other) { return value.compareTo(other.value); }
    @Override public String toString() { return value; }
}

