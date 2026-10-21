package io.github.kxyr0.echoplayersleep.api.world;

public record Vector3(double x, double y, double z) {
    public Vector3 {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("vector coordinates must be finite");
        }
    }
}
