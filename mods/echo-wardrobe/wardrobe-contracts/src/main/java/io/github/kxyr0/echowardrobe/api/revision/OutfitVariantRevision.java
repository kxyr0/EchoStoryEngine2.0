package io.github.kxyr0.echowardrobe.api.revision;

/**
 * Monotonic revision of an exact outfit variant definition.
 */
public record OutfitVariantRevision(long value) {
    public OutfitVariantRevision {
        if (value < 0L) {
            throw new IllegalArgumentException("Outfit variant revision cannot be negative.");
        }
    }
}
