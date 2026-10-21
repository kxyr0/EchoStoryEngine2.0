package io.github.kxyr0.echocutscene.core.recording;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echocutscene.api.camera.CameraPose;
import io.github.kxyr0.echocutscene.api.camera.CameraTrackMetadata;
import io.github.kxyr0.echocutscene.api.camera.CameraTrackSnapshot;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackDataId;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackId;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackRevision;
import java.util.List;
import org.junit.jupiter.api.Test;

class CameraRecordingTest {
    @Test
    void variableFpsSamplesResampleToFixedRate() {
        List<CameraRecordingSample> raw = List.of(
                new CameraRecordingSample(0L, CameraPose.of(0.0D, 64.0D, 0.0D, 0.0D, 70.0D)),
                new CameraRecordingSample(37L, CameraPose.of(1.0D, 64.0D, 0.0D, 10.0D, 70.0D)),
                new CameraRecordingSample(1000L, CameraPose.of(10.0D, 64.0D, 0.0D, 90.0D, 70.0D)));

        CameraTrackSnapshot snapshot = new CameraSampleResampler().resample(metadata(1000L, 20), raw);

        assertEquals(21, snapshot.keyframes().size());
        assertEquals(0L, snapshot.keyframes().get(0).timeMillis());
        assertEquals(1000L, snapshot.keyframes().get(20).timeMillis());
    }

    @Test
    void simplifierPreservesDurationEndpoints() {
        CameraTrackSnapshot snapshot = new CameraSampleResampler().resample(metadata(1000L, 20), List.of(
                new CameraRecordingSample(0L, CameraPose.of(0.0D, 64.0D, 0.0D, 0.0D, 70.0D)),
                new CameraRecordingSample(1000L, CameraPose.of(10.0D, 64.0D, 0.0D, 90.0D, 70.0D))));

        CameraTrackSnapshot simplified = new CameraTrackSimplifier().simplify(snapshot, 100.0D, 100.0D);

        assertEquals(2, simplified.keyframes().size());
        assertEquals(0L, simplified.keyframes().get(0).timeMillis());
        assertEquals(1000L, simplified.keyframes().get(1).timeMillis());
        assertTrue(simplified.keyframes().size() < snapshot.keyframes().size());
    }

    private static CameraTrackMetadata metadata(long duration, int sampleRate) {
        return new CameraTrackMetadata(CameraTrackId.parse("jdh:recording"), new CameraTrackRevision(1L),
                new CameraTrackDataId("recording"), duration, sampleRate, 1, "fingerprint");
    }
}
