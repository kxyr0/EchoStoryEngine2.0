package io.github.kxyr0.echoanimation.api.sync;

/**
 * Monotonic animation sync revision sent by the server authority.
 */
public record AnimationSyncRevision(long value) implements Comparable<AnimationSyncRevision> {
    public AnimationSyncRevision {
        if (value <= 0L) {
            throw new IllegalArgumentException("AnimationSyncRevision must be positive.");
        }
    }

    public AnimationSyncRevision next() {
        return new AnimationSyncRevision(value + 1L);
    }

    @Override
    public int compareTo(AnimationSyncRevision other) {
        return Long.compare(value, other.value);
    }
}
