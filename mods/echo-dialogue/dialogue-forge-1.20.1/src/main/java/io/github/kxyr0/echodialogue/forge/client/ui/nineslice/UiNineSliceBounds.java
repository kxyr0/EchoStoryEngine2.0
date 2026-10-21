package io.github.kxyr0.echodialogue.forge.client.ui.nineslice;

/**
 * Target GUI rectangle for a 9-slice draw call.
 */
public record UiNineSliceBounds(int x, int y, int width, int height) {
    public UiNineSliceBounds {
        requirePositive(width, "width");
        requirePositive(height, "height");
    }

    public static UiNineSliceBounds fromEdges(int left, int top, int right, int bottom) {
        return new UiNineSliceBounds(left, top, right - left, bottom - top);
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException("9-slice target " + name + " must be positive.");
        }
    }
}
