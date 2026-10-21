package io.github.kxyr0.echoplayer.forge.api.appearance.resolved;

/**
 * Monotonic public revision for resolved appearance publication.
 */
public record PlayerAppearanceRevision(long value) {
    public PlayerAppearanceRevision {
        if (value < 0L) {
            throw new IllegalArgumentException("Appearance revision cannot be negative.");
        }
    }
}
