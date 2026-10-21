package io.github.kxyr0.echovisibility.api.identity;

public record VisibilityWatchGeneration(long value) implements Comparable<VisibilityWatchGeneration> {
    public VisibilityWatchGeneration {
        if (value <= 0L) {
            throw new IllegalArgumentException("Visibility watch generation must be positive.");
        }
    }

    @Override
    public int compareTo(VisibilityWatchGeneration other) {
        return Long.compare(value, other.value);
    }
}
