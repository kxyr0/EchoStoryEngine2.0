package io.github.kxyr0.echodialogue.forge.client.ui.image;

/**
 * Fixed source borders for sliced and tiled UI images.
 */
public record UiSliceBorders(int left, int right, int top, int bottom) {
    public UiSliceBorders {
        requireNonNegative(left, "left");
        requireNonNegative(right, "right");
        requireNonNegative(top, "top");
        requireNonNegative(bottom, "bottom");
    }

    public static UiSliceBorders none() {
        return new UiSliceBorders(0, 0, 0, 0);
    }

    public int minimumWidth() {
        return left + right;
    }

    public int minimumHeight() {
        return top + bottom;
    }

    public boolean empty() {
        return minimumWidth() == 0 && minimumHeight() == 0;
    }

    public void validateInside(int sourceWidth, int sourceHeight) {
        if (minimumWidth() > sourceWidth) {
            throw new IllegalArgumentException("UI_IMAGE_BORDERS_INVALID: left+right exceeds source width.");
        }
        if (minimumHeight() > sourceHeight) {
            throw new IllegalArgumentException("UI_IMAGE_BORDERS_INVALID: top+bottom exceeds source height.");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException("UI_IMAGE_BORDERS_INVALID: " + name + " must be non-negative.");
        }
    }
}
