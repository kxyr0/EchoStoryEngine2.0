package io.github.kxyr0.echoanimationeditor.api.camera;

public record AnimatorCameraSettings(double baseSpeed, double slowMultiplier, double fastMultiplier) {
    public static AnimatorCameraSettings defaults() {
        return new AnimatorCameraSettings(0.25D, 0.25D, 4.0D);
    }

    public AnimatorCameraSettings {
        if (baseSpeed <= 0.0D || slowMultiplier <= 0.0D || fastMultiplier <= 0.0D) {
            throw new IllegalArgumentException("camera speeds must be positive");
        }
    }
}
