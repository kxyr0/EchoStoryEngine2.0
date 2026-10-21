package io.github.kxyr0.echowardrobe.api.revision;

/**
 * Monotonic asset revision for extracted overlays, face layers, and models.
 */
public record WearableAssetRevision(long value) {
    public WearableAssetRevision {
        if (value < 0L) {
            throw new IllegalArgumentException("Asset revision must not be negative.");
        }
    }

    public WearableAssetRevision next() {
        return new WearableAssetRevision(value + 1L);
    }
}
