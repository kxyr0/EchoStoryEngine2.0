package io.github.kxyr0.echodialogue.forge.client.ui.image;

/**
 * Source rectangle inside a texture image.
 */
public record UiTextureRegion(
        int x,
        int y,
        int width,
        int height,
        int fullTextureWidth,
        int fullTextureHeight
) {
    public UiTextureRegion {
        requireNonNegative(x, "x");
        requireNonNegative(y, "y");
        requirePositive(width, "width");
        requirePositive(height, "height");
        requirePositive(fullTextureWidth, "fullTextureWidth");
        requirePositive(fullTextureHeight, "fullTextureHeight");
        if (x + width > fullTextureWidth || y + height > fullTextureHeight) {
            throw new IllegalArgumentException("UI_IMAGE_REGION_INVALID: source region exceeds texture bounds.");
        }
    }

    public static UiTextureRegion full(int width, int height) {
        return new UiTextureRegion(0, 0, width, height, width, height);
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException("UI_IMAGE_SOURCE_SIZE_INVALID: " + name + " must be positive.");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException("UI_IMAGE_REGION_INVALID: " + name + " must be non-negative.");
        }
    }
}
