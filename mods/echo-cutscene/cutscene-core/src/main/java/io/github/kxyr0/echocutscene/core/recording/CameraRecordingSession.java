package io.github.kxyr0.echocutscene.core.recording;

import io.github.kxyr0.echocutscene.api.camera.CameraPose;
import java.util.ArrayList;
import java.util.List;

public final class CameraRecordingSession {
    private final long startedAtMillis;
    private final List<CameraRecordingSample> samples = new ArrayList<>();
    private boolean recording = true;

    public CameraRecordingSession(long startedAtMillis) {
        this.startedAtMillis = startedAtMillis;
    }

    public void sample(long nowMillis, CameraPose pose) {
        if (!recording) {
            throw new IllegalStateException("Recording is not active.");
        }
        samples.add(new CameraRecordingSample(Math.max(0L, nowMillis - startedAtMillis), pose));
    }

    public List<CameraRecordingSample> stop() {
        recording = false;
        if (samples.size() < 2) {
            throw new IllegalStateException("CUTSCENE_RECORDING_TOO_SHORT");
        }
        return List.copyOf(samples);
    }

    public boolean recording() {
        return recording;
    }
}
