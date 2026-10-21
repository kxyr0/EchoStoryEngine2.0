package io.github.kxyr0.echocutscene.core.binding;

import io.github.kxyr0.echocutscene.api.camera.CameraKeyframe;
import io.github.kxyr0.echocutscene.api.camera.CameraTrackSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CameraTrackBinder {
    public List<BindingDiagnostic> validate(CameraTrackSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        List<BindingDiagnostic> diagnostics = new ArrayList<>();
        long previous = -1L;
        for (CameraKeyframe frame : snapshot.keyframes()) {
            if (frame.timeMillis() <= previous) {
                diagnostics.add(new BindingDiagnostic("CUTSCENE_TRACK_INVALID",
                        "Camera keyframe timestamps must be strictly increasing."));
            }
            previous = frame.timeMillis();
        }
        return List.copyOf(diagnostics);
    }
}
