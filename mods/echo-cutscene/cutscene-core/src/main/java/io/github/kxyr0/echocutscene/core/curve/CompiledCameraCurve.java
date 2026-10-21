package io.github.kxyr0.echocutscene.core.curve;

import io.github.kxyr0.echocutscene.api.camera.CameraKeyframe;
import java.util.List;
import java.util.Objects;

public record CompiledCameraCurve(List<CameraKeyframe> keyframes, long durationMillis) {
    public CompiledCameraCurve {
        keyframes = List.copyOf(Objects.requireNonNull(keyframes, "keyframes"));
        if (keyframes.size() < 2 || durationMillis < 0L) {
            throw new IllegalArgumentException("CompiledCameraCurve requires keyframes and duration.");
        }
    }
}
