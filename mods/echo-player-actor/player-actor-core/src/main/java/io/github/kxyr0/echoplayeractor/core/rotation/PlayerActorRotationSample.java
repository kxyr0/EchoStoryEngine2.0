package io.github.kxyr0.echoplayeractor.core.rotation;

public record PlayerActorRotationSample(double yaw, double pitch, boolean complete) {
    public PlayerActorRotationSample {
        if (!Double.isFinite(yaw) || !Double.isFinite(pitch)) {
            throw new IllegalArgumentException("Rotation sample must be finite.");
        }
    }
}
