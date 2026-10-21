package io.github.kxyr0.echothoughtspace.core.canvas;

public record CanvasPoint(double x, double y) {
    public CanvasPoint {
        if (!Double.isFinite(x) || !Double.isFinite(y)) throw new IllegalArgumentException("Point must be finite.");
    }

    public CanvasPoint plus(CanvasVector value) { return new CanvasPoint(x + value.x(), y + value.y()); }
    public CanvasVector minus(CanvasPoint value) { return new CanvasVector(x - value.x(), y - value.y()); }
}

