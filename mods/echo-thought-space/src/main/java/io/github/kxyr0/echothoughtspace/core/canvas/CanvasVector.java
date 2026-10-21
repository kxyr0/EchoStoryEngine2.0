package io.github.kxyr0.echothoughtspace.core.canvas;

public record CanvasVector(double x, double y) {
    public CanvasVector {
        if (!Double.isFinite(x) || !Double.isFinite(y)) throw new IllegalArgumentException("Vector must be finite.");
    }

    public CanvasVector scale(double value) { return new CanvasVector(x * value, y * value); }
}

