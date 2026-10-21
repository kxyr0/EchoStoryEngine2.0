package io.github.kxyr0.echoteleportmagic.api;

public record TeleportAnchor(double x, double y, double z, float yaw, float width, float height) {
    public TeleportAnchor {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw) || !Float.isFinite(width) || !Float.isFinite(height)
                || width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid visual bounds");
    }
}
