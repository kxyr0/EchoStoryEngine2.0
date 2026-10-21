package io.github.kxyr0.echocutscene.core.curve;

import io.github.kxyr0.echocutscene.api.camera.CameraTrackSnapshot;
import java.util.Objects;

public final class CameraCurveCompiler {
    public CompiledCameraCurve compile(CameraTrackSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return new CompiledCameraCurve(RotationUnwrapper.unwrap(snapshot.keyframes()),
                snapshot.metadata().durationMillis());
    }
}
