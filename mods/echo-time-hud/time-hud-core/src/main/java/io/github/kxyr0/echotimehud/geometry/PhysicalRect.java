package io.github.kxyr0.echotimehud.geometry;

public record PhysicalRect(double x, double y, double width, double height) {
    public PhysicalRect {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(width) || !Double.isFinite(height)
                || width < 0.0 || height < 0.0) throw new IllegalArgumentException("Invalid physical rectangle.");
    }

    public double right() { return x + width; }
    public double bottom() { return y + height; }
}

