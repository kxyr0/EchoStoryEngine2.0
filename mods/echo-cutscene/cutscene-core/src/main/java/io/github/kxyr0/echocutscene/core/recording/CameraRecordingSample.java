package io.github.kxyr0.echocutscene.core.recording;

import io.github.kxyr0.echocutscene.api.camera.CameraPose;
import java.util.Objects;

public record CameraRecordingSample(long timeMillis, CameraPose pose) {
    public CameraRecordingSample {
        if (timeMillis < 0L) {
            throw new IllegalArgumentException("timeMillis must not be negative.");
        }
        pose = Objects.requireNonNull(pose, "pose");
    }
}
