package io.github.kxyr0.echocutscene.api.camera;

import io.github.kxyr0.echocutscene.api.camera.curve.CameraInterpolation;
import java.util.Objects;

public record CameraKeyframe(
        long timeMillis,
        CameraPose pose,
        CameraInterpolation interpolation
) {
    public CameraKeyframe {
        if (timeMillis < 0L) {
            throw new IllegalArgumentException("timeMillis must not be negative.");
        }
        pose = Objects.requireNonNull(pose, "pose");
        interpolation = Objects.requireNonNull(interpolation, "interpolation");
    }
}
