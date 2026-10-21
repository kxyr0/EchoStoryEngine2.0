package io.github.kxyr0.echovisibility.api.geometry;

public record VisibilityScreenRectangle(double minX, double minY, double maxX, double maxY) {
    public VisibilityScreenRectangle {
        if (!Double.isFinite(minX) || !Double.isFinite(minY)
                || !Double.isFinite(maxX) || !Double.isFinite(maxY)
                || minX > maxX || minY > maxY) {
            throw new IllegalArgumentException("Screen rectangle must be finite and normalized.");
        }
    }

    public boolean intersectsViewport() {
        return maxX >= -1.0D && minX <= 1.0D && maxY >= -1.0D && minY <= 1.0D;
    }
}
