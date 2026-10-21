package io.github.kxyr0.echomagic.core.gesture;

public record MagicGesturePoint(double x, double y, long elapsedMillis) {
    public MagicGesturePoint {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("Gesture coordinates must be finite.");
        }
        if (elapsedMillis < 0L) throw new IllegalArgumentException("Gesture time must not be negative.");
    }

    public double distanceTo(MagicGesturePoint other) {
        return Math.hypot(x - other.x, y - other.y);
    }
}
