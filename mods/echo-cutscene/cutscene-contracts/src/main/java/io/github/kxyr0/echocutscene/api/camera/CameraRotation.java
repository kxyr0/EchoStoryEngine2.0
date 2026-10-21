package io.github.kxyr0.echocutscene.api.camera;

public record CameraRotation(double yaw, double pitch, double roll) {
    public CameraRotation {
        finite(yaw, "yaw");
        finite(pitch, "pitch");
        finite(roll, "roll");
        if (pitch < -90.0D || pitch > 90.0D) {
            throw new IllegalArgumentException("pitch must be in [-90, 90].");
        }
    }

    private static void finite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }
}
