package io.github.kxyr0.echodialogue.forge.client.ui.image;

/**
 * Target GUI rectangle for a UI image draw call.
 */
public record UiImageBounds(int x, int y, int width, int height) {
    public UiImageBounds {
        requirePositive(width, "width");
        requirePositive(height, "height");
    }

    public static UiImageBounds fromEdges(int left, int top, int right, int bottom) {
        return new UiImageBounds(left, top, right - left, bottom - top);
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException("UI_IMAGE_DESTINATION_TOO_SMALL: " + name + " must be positive.");
        }
    }
}
