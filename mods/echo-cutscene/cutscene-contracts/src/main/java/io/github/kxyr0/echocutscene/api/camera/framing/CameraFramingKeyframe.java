package io.github.kxyr0.echocutscene.api.camera.framing;

import java.util.Objects;

public record CameraFramingKeyframe(long timeMillis, CameraFraming framing) {
    public CameraFramingKeyframe {
        if (timeMillis < 0L) {
            throw new IllegalArgumentException("timeMillis must not be negative.");
        }
        framing = Objects.requireNonNull(framing, "framing");
    }
}
