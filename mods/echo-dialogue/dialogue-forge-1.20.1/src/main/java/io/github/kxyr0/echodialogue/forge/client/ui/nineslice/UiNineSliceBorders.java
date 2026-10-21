package io.github.kxyr0.echodialogue.forge.client.ui.nineslice;

/**
 * Immutable source-texture borders that stay unscaled in a 9-slice panel.
 */
public record UiNineSliceBorders(int left, int right, int top, int bottom) {
    public UiNineSliceBorders {
        requireNonNegative(left, "left");
        requireNonNegative(right, "right");
        requireNonNegative(top, "top");
        requireNonNegative(bottom, "bottom");
    }

    public int minimumWidth() {
        return left + right;
    }

    public int minimumHeight() {
        return top + bottom;
    }

    void validateInside(int sourceWidth, int sourceHeight) {
        if (minimumWidth() >= sourceWidth) {
            throw new IllegalArgumentException("9-slice left+right must leave at least 1 source center pixel.");
        }
        if (minimumHeight() >= sourceHeight) {
            throw new IllegalArgumentException("9-slice top+bottom must leave at least 1 source center pixel.");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException("9-slice border " + name + " must be non-negative.");
        }
    }
}
