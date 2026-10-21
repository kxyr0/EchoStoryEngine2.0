package io.github.kxyr0.echowardrobe.api.contribution;

/**
 * Pixel strip copied from an authored 64x64 skin onto an 8x8 dialogue head.
 */
public record WearablePortraitStrip(
        int sourceX,
        int sourceY,
        int targetX,
        int targetY,
        int width,
        int height,
        boolean outerLayer,
        double pixelXOffset,
        double pixelYOffset,
        double pixelLeftInset,
        double pixelRightInset,
        double targetHeightScale
) {
    public WearablePortraitStrip {
        requireNonNegative(sourceX, "sourceX");
        requireNonNegative(sourceY, "sourceY");
        requireNonNegative(targetX, "targetX");
        requireNonNegative(targetY, "targetY");
        requirePositive(width, "width");
        requirePositive(height, "height");
        requireFinite(pixelXOffset, "pixelXOffset");
        requireFinite(pixelYOffset, "pixelYOffset");
        requireFinite(pixelLeftInset, "pixelLeftInset");
        requireFinite(pixelRightInset, "pixelRightInset");
        requireNonNegative(targetHeightScale, "targetHeightScale");
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative.");
        }
    }

    private static void requireNonNegative(double value, String name) {
        requireFinite(value, name);
        if (value < 0D) {
            throw new IllegalArgumentException(name + " must be non-negative.");
        }
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }
}
