package io.github.kxyr0.echocutscene.api.camera;

public record CameraFieldOfView(double degrees) {
    public CameraFieldOfView {
        if (!Double.isFinite(degrees) || degrees < 1.0D || degrees > 179.0D) {
            throw new IllegalArgumentException("CameraFieldOfView must be finite and in [1, 179].");
        }
    }

    public static CameraFieldOfView normal() {
        return new CameraFieldOfView(70.0D);
    }
}
