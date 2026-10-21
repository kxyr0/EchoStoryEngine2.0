package io.github.kxyr0.echocutscene.forge.client.recording;

import io.github.kxyr0.echocutscene.api.camera.CameraTrackMetadata;
import io.github.kxyr0.echocutscene.api.camera.CameraTrackSnapshot;
import io.github.kxyr0.echocutscene.api.camera.framing.CameraFramingKeyframe;
import io.github.kxyr0.echocutscene.api.camera.mode.CameraModeKeyframe;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetKeyframe;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackDataId;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackId;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackRevision;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.core.recording.CameraRecordingSample;
import io.github.kxyr0.echocutscene.core.recording.CameraSampleResampler;
import io.github.kxyr0.echocutscene.core.recording.CameraTrackSimplifier;
import io.github.kxyr0.echocutscene.core.recording.CameraTrackSourceFormatter;
import java.util.List;
import net.minecraft.Util;

public final class ClientCameraTrackRecorder {
    private final CameraSampleResampler resampler = new CameraSampleResampler();
    private final CameraTrackSimplifier simplifier = new CameraTrackSimplifier();
    private final CameraTrackSourceFormatter formatter = new CameraTrackSourceFormatter();

    public String source(
            CutsceneId cutsceneId,
            List<CameraRecordingSample> raw,
            List<CameraModeKeyframe> modes,
            List<CameraTargetKeyframe> targets,
            List<CameraFramingKeyframe> framings
    ) {
        CameraTrackSnapshot snapshot = simplifier.simplify(resampler.resample(metadata(cutsceneId, raw), raw),
                0.02D, 0.25D);
        CameraTrackSnapshot semantic = new CameraTrackSnapshot(snapshot.metadata(), snapshot.keyframes(),
                modes, targets, framings);
        return formatter.format(semantic);
    }

    private static CameraTrackMetadata metadata(CutsceneId cutsceneId, List<CameraRecordingSample> raw) {
        long duration = raw.get(raw.size() - 1).timeMillis();
        return new CameraTrackMetadata(CameraTrackId.parse(cutsceneId.value() + "_camera"),
                new CameraTrackRevision(Util.getMillis()), new CameraTrackDataId(cutsceneId.value() + ".camera.es"),
                duration, 60, 2, fingerprint(raw));
    }

    private static String fingerprint(List<CameraRecordingSample> raw) {
        long duration = raw.get(raw.size() - 1).timeMillis();
        return Long.toHexString(duration) + "-" + Integer.toHexString(raw.size());
    }
}
