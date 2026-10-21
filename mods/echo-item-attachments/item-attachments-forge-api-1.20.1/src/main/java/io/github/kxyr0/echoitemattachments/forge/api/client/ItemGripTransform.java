package io.github.kxyr0.echoitemattachments.forge.api.client;

public record ItemGripTransform(
        double x,
        double y,
        double z,
        float rotateX,
        float rotateY,
        float rotateZ,
        float scale) {
    public ItemGripTransform {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(rotateX) || !Float.isFinite(rotateY)
                || !Float.isFinite(rotateZ) || !Float.isFinite(scale) || scale <= 0.0F) {
            throw new IllegalArgumentException("grip transform must be finite and scale positive");
        }
    }
}
