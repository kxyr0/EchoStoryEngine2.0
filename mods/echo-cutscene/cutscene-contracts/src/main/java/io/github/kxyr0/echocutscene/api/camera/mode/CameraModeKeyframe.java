package io.github.kxyr0.echocutscene.api.camera.mode;

import java.util.Objects;

public record CameraModeKeyframe(long timeMillis, CameraControlMode mode) {
    public CameraModeKeyframe {
        if (timeMillis < 0L) {
            throw new IllegalArgumentException("timeMillis must not be negative.");
        }
        mode = Objects.requireNonNull(mode, "mode");
    }
}
