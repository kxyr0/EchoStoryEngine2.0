package io.github.kxyr0.echonpc.api.form.body;

/**
 * Server-authoritative collision and eye dimensions for a form.
 */
public record NpcBodyDimensions(double width, double height, double eyeHeight) {
    public NpcBodyDimensions {
        require(width > 0.05D && width <= 4.0D, "width");
        require(height > 0.05D && height <= 8.0D, "height");
        require(eyeHeight > 0.0D && eyeHeight <= height, "eyeHeight");
    }

    private static void require(boolean condition, String name) {
        if (!condition) {
            throw new IllegalArgumentException("NpcBodyDimensions " + name + " is invalid.");
        }
    }
}
