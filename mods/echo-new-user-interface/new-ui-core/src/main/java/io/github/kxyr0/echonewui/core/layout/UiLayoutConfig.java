package io.github.kxyr0.echonewui.core.layout;

public record UiLayoutConfig(
        UiReferenceCanvas referenceCanvas,
        int referenceBookWidth,
        int referenceBookHeight,
        int referenceSafeMargin) {

    public static final int DEFAULT_BOOK_WIDTH = 784;
    public static final int DEFAULT_BOOK_HEIGHT = 612;
    public static final int DEFAULT_SAFE_MARGIN = 24;

    public UiLayoutConfig {
        if (referenceCanvas == null) {
            throw new IllegalArgumentException("referenceCanvas must not be null");
        }
        requirePositive(referenceBookWidth, "referenceBookWidth");
        requirePositive(referenceBookHeight, "referenceBookHeight");
        if (referenceSafeMargin < 0) {
            throw new IllegalArgumentException("referenceSafeMargin must be non-negative");
        }
    }

    public static UiLayoutConfig defaults() {
        return new UiLayoutConfig(UiReferenceCanvas.DEFAULT, DEFAULT_BOOK_WIDTH,
                DEFAULT_BOOK_HEIGHT, DEFAULT_SAFE_MARGIN);
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive: " + value);
        }
    }
}
