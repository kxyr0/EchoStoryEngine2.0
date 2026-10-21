package io.github.kxyr0.echovisibility.api.identity;

public record VisibilityDefinitionRevision(long value) implements Comparable<VisibilityDefinitionRevision> {
    public VisibilityDefinitionRevision {
        if (value <= 0L) {
            throw new IllegalArgumentException("Visibility definition revision must be positive.");
        }
    }

    @Override
    public int compareTo(VisibilityDefinitionRevision other) {
        return Long.compare(value, other.value);
    }
}
