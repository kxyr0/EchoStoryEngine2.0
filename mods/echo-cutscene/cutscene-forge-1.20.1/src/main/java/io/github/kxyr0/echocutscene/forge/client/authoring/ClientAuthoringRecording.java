package io.github.kxyr0.echocutscene.forge.client.authoring;

import io.github.kxyr0.echocutscene.api.camera.CameraPose;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.core.recording.CameraRecordingSample;
import io.github.kxyr0.echocutscene.core.recording.CameraRecordingSession;
import io.github.kxyr0.echocutscene.forge.client.recording.ClientCameraTrackRecorder;
import io.github.kxyr0.echocutscene.forge.network.EchoCutsceneNetwork;
import io.github.kxyr0.echocutscene.forge.network.authoring.ServerboundCameraRecordingSavePacket;
import java.util.List;
import net.minecraft.Util;

public final class ClientAuthoringRecording {
    private final ClientCameraTrackRecorder trackRecorder = new ClientCameraTrackRecorder();
    private CameraRecordingSession recording;
    private int sampleCount;
    private long lastSampleAtMillis = -1L;

    public void toggle(String sessionId, CutsceneId cutsceneId, CameraPose pose,
            ClientAuthoringSemanticState semantic) {
        if (recording == null) {
            start(pose, semantic);
        } else {
            stop(sessionId, cutsceneId, pose, semantic);
        }
    }

    public void sample(long nowMillis, CameraPose pose, ClientAuthoringSemanticState semantic) {
        if (recording == null || nowMillis <= lastSampleAtMillis) {
            return;
        }
        semantic.recordFramingSample(nowMillis);
        recording.sample(nowMillis, pose);
        lastSampleAtMillis = nowMillis;
        sampleCount++;
    }

    public boolean active() {
        return recording != null;
    }

    public int sampleCount() {
        return sampleCount;
    }

    public void reset() {
        recording = null;
        sampleCount = 0;
        lastSampleAtMillis = -1L;
    }

    private void start(CameraPose pose, ClientAuthoringSemanticState semantic) {
        long now = Util.getMillis();
        recording = new CameraRecordingSession(now);
        semantic.beginRecording(now);
        sampleCount = 0;
        lastSampleAtMillis = -1L;
        sample(now, pose, semantic);
    }

    private void stop(String sessionId, CutsceneId cutsceneId, CameraPose pose,
            ClientAuthoringSemanticState semantic) {
        sample(Util.getMillis(), pose, semantic);
        try {
            List<CameraRecordingSample> raw = recording.stop();
            sendTrack(sessionId, cutsceneId, raw, semantic);
        } catch (IllegalStateException exception) {
            sampleCount = 0;
        } finally {
            semantic.endRecording();
            recording = null;
        }
    }

    private void sendTrack(String sessionId, CutsceneId cutsceneId, List<CameraRecordingSample> raw,
            ClientAuthoringSemanticState semantic) {
        String source = trackRecorder.source(cutsceneId, raw, semantic.recordedModes(),
                semantic.recordedTargets(), semantic.recordedFramings());
        EchoCutsceneNetwork.sendToServer(new ServerboundCameraRecordingSavePacket(sessionId, cutsceneId, source));
    }
}
