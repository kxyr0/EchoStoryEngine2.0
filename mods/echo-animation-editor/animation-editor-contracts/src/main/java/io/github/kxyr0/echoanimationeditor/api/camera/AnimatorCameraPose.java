package io.github.kxyr0.echoanimationeditor.api.camera;

public record AnimatorCameraPose(double x, double y, double z, float yaw, float pitch) {
    public AnimatorCameraPose move(double dx, double dy, double dz) {
        return new AnimatorCameraPose(x + dx, y + dy, z + dz, yaw, pitch);
    }

    public AnimatorCameraPose rotate(float yawDelta, float pitchDelta) {
        float nextPitch = Math.max(-89.0F, Math.min(89.0F, pitch + pitchDelta));
        return new AnimatorCameraPose(x, y, z, yaw + yawDelta, nextPitch);
    }
}
