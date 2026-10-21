package io.github.kxyr0.echowardrobe.api.revision;

/**
 * Monotonic revision of an accepted wardrobe catalog snapshot.
 */
public record WardrobeCatalogRevision(long value) {
    public WardrobeCatalogRevision {
        if (value < 0L) {
            throw new IllegalArgumentException("Catalog revision cannot be negative.");
        }
    }

    public WardrobeCatalogRevision next() {
        return new WardrobeCatalogRevision(value + 1L);
    }
}
