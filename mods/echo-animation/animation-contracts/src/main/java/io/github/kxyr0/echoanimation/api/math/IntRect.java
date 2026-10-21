package io.github.kxyr0.echoanimation.api.math;

/**
 * Immutable pixel rectangle.
 */
public record IntRect(int x, int y, int width, int height) {
    public IntRect {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("IntRect width/height должны быть положительными.");
        }
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("IntRect x/y не могут быть отрицательными.");
        }
    }

    public boolean fitsInside(int atlasWidth, int atlasHeight) {
        return x + width <= atlasWidth && y + height <= atlasHeight;
    }
}

