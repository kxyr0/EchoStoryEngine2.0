package io.github.kxyr0.echovisibility.core.math;

public record VisibilityClipPoint(double x, double y, double z, double w) {
    public boolean valid() {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) && Double.isFinite(w);
    }

    public boolean inFront() {
        return valid() && w > 1.0E-7D;
    }

    public double ndcX() {
        return x / w;
    }

    public double ndcY() {
        return y / w;
    }

    public double ndcZ() {
        return z / w;
    }

    public boolean insideViewport(double marginX, double marginY) {
        return inFront() && ndcX() >= -1.0D - marginX && ndcX() <= 1.0D + marginX
                && ndcY() >= -1.0D - marginY && ndcY() <= 1.0D + marginY
                && ndcZ() >= -1.0D && ndcZ() <= 1.0D;
    }
}
