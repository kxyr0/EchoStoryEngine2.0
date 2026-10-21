package io.github.kxyr0.echonewui.core.layout;

public record UiRectangle(int x, int y, int width, int height) {
    public UiRectangle {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("rectangle origin must be non-negative: " + x + "," + y);
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("rectangle size must be positive: " + width + "x" + height);
        }
    }

    public static UiRectangle fromEdges(int left, int top, int right, int bottom) {
        return new UiRectangle(left, top, right - left, bottom - top);
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public double centerX() {
        return x + width / 2.0D;
    }

    public double centerY() {
        return y + height / 2.0D;
    }

    public boolean clippedBy(int boundaryWidth, int boundaryHeight) {
        return x < 0 || y < 0 || right() > boundaryWidth || bottom() > boundaryHeight;
    }
}
