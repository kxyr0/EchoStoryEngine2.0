package io.github.kxyr0.echoanimation.forge.client.portrait;

/**
 * Optional per-layer projection overrides for dialogue portraits.
 */
public record PortraitLayerConfig(
        Double placeX,
        Double placeY,
        Double offsetX,
        Double offsetY,
        Double scaleX,
        Double scaleY,
        Double width,
        Double height,
        Boolean visible
) {
    private static final PortraitLayerConfig IDENTITY =
            new PortraitLayerConfig(null, null, null, null, null, null, null, null, null);

    public PortraitLayerConfig {
        requireFinite(placeX, "placeX");
        requireFinite(placeY, "placeY");
        requireFinite(offsetX, "offsetX");
        requireFinite(offsetY, "offsetY");
        requirePositive(scaleX, "scaleX");
        requirePositive(scaleY, "scaleY");
        requirePositive(width, "w");
        requirePositive(height, "h");
    }

    public static PortraitLayerConfig identity() {
        return IDENTITY;
    }

    public double placeX(double fallback) {
        return placeX == null ? fallback : placeX.doubleValue();
    }

    public double placeY(double fallback) {
        return placeY == null ? fallback : placeY.doubleValue();
    }

    public double resolvedOffsetX() {
        return offsetX == null ? 0.0D : offsetX.doubleValue();
    }

    public double resolvedOffsetY() {
        return offsetY == null ? 0.0D : offsetY.doubleValue();
    }

    public double resolvedScaleX() {
        return scaleX == null ? 1.0D : scaleX.doubleValue();
    }

    public double resolvedScaleY() {
        return scaleY == null ? 1.0D : scaleY.doubleValue();
    }

    public int width(int fallback) {
        return width == null ? fallback : Math.max(1, (int) Math.round(width.doubleValue()));
    }

    public int height(int fallback) {
        return height == null ? fallback : Math.max(1, (int) Math.round(height.doubleValue()));
    }

    public boolean isVisible() {
        return visible == null || visible.booleanValue();
    }

    private static void requireFinite(Double value, String name) {
        if (value != null && !Double.isFinite(value.doubleValue())) {
            throw new IllegalArgumentException(name + " должен быть finite.");
        }
    }

    private static void requirePositive(Double value, String name) {
        requireFinite(value, name);
        if (value != null && value.doubleValue() <= 0.0D) {
            throw new IllegalArgumentException(name + " должен быть положительным.");
        }
    }
}
