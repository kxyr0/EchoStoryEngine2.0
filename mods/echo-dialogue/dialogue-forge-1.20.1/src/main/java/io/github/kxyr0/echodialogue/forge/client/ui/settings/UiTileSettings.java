package io.github.kxyr0.echodialogue.forge.client.ui.settings;

/**
 * Tile scale, grid origin, and edge handling for repeated UI image regions.
 */
public record UiTileSettings(float scaleX, float scaleY, int originX, int originY, boolean fullOnly) {
    public static final int MAX_TILES_PER_IMAGE = 4096;
    private static final UiTileSettings DEFAULTS = new UiTileSettings(1.0F, 1.0F, 0, 0, false);

    public UiTileSettings {
        requirePositive(scaleX, "scaleX");
        requirePositive(scaleY, "scaleY");
    }

    public static UiTileSettings defaults() {
        return DEFAULTS;
    }

    private static void requirePositive(float value, String name) {
        if (!Float.isFinite(value) || value <= 0.0F) {
            throw new IllegalArgumentException("UI_IMAGE_TILE_SCALE_INVALID: " + name + " must be positive.");
        }
    }
}
