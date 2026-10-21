package io.github.kxyr0.echowardrobe.api.revision;

/**
 * Monotonic definition revision published by the wardrobe catalog.
 */
public record WearableDefinitionRevision(long value) {
    public WearableDefinitionRevision {
        if (value < 0L) {
            throw new IllegalArgumentException("Definition revision must not be negative.");
        }
    }

    public WearableDefinitionRevision next() {
        return new WearableDefinitionRevision(value + 1L);
    }
}
