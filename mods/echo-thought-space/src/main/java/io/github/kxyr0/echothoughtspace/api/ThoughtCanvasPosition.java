package io.github.kxyr0.echothoughtspace.api;

public record ThoughtCanvasPosition(double x, double y) {
    public ThoughtCanvasPosition {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("Canvas position must be finite.");
        }
    }
}

