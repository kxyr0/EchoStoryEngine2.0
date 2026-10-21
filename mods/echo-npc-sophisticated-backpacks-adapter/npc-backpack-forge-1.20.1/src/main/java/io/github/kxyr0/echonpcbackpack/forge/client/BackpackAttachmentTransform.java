package io.github.kxyr0.echonpcbackpack.forge.client;

public record BackpackAttachmentTransform(float x, float y, float z,
        float rotateX, float rotateY, float rotateZ, float scale) {
    public BackpackAttachmentTransform {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)
                || !Float.isFinite(rotateX) || !Float.isFinite(rotateY) || !Float.isFinite(rotateZ)
                || !Float.isFinite(scale) || scale <= 0.0F) {
            throw new IllegalArgumentException("Attachment transform values must be finite and scale positive.");
        }
    }
}
