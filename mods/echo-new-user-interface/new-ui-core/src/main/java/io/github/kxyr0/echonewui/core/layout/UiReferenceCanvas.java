package io.github.kxyr0.echonewui.core.layout;

public record UiReferenceCanvas(int width, int height) {
    public static final UiReferenceCanvas DEFAULT = new UiReferenceCanvas(1920, 1080);

    public UiReferenceCanvas {
        requirePositive(width, "width");
        requirePositive(height, "height");
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive: " + value);
        }
    }
}
