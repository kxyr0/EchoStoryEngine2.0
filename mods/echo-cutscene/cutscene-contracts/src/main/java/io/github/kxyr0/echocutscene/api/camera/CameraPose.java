package io.github.kxyr0.echocutscene.api.camera;

import java.util.Objects;

public record CameraPose(
        CameraPosition position,
        CameraRotation rotation,
        CameraFieldOfView fieldOfView
) {
    public CameraPose {
        position = Objects.requireNonNull(position, "position");
        rotation = Objects.requireNonNull(rotation, "rotation");
        fieldOfView = Objects.requireNonNull(fieldOfView, "fieldOfView");
    }

    public static CameraPose of(double x, double y, double z, double yaw, double fov) {
        return new CameraPose(CameraPosition.of(x, y, z), new CameraRotation(yaw, 0.0D, 0.0D),
                new CameraFieldOfView(fov));
    }
}
