package io.github.kxyr0.echocutscene.core.recording;

import io.github.kxyr0.echocutscene.api.camera.CameraKeyframe;
import io.github.kxyr0.echocutscene.api.camera.CameraTrackMetadata;
import io.github.kxyr0.echocutscene.api.camera.CameraTrackSnapshot;
import io.github.kxyr0.echocutscene.api.camera.curve.CameraInterpolation;
import io.github.kxyr0.echocutscene.core.curve.CameraPoseEvaluator;
import io.github.kxyr0.echocutscene.core.curve.CompiledCameraCurve;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CameraSampleResampler {
    public CameraTrackSnapshot resample(CameraTrackMetadata metadata, List<CameraRecordingSample> samples) {
        Objects.requireNonNull(metadata, "metadata");
        List<CameraRecordingSample> source = List.copyOf(Objects.requireNonNull(samples, "samples"));
        List<CameraKeyframe> rawFrames = source.stream()
                .map(sample -> new CameraKeyframe(sample.timeMillis(), sample.pose(), CameraInterpolation.LINEAR))
                .toList();
        return new CameraTrackSnapshot(metadata, fixed(rawFrames, metadata));
    }

    private List<CameraKeyframe> fixed(List<CameraKeyframe> rawFrames, CameraTrackMetadata metadata) {
        CameraPoseEvaluator evaluator = new CameraPoseEvaluator();
        CompiledCameraCurve curve = new CompiledCameraCurve(rawFrames, metadata.durationMillis());
        List<CameraKeyframe> output = new ArrayList<>();
        long step = Math.max(1L, Math.round(1000.0D / metadata.sampleRate()));
        for (long time = 0L; time < metadata.durationMillis(); time += step) {
            output.add(new CameraKeyframe(time, evaluator.evaluate(curve, time, 0.0D), CameraInterpolation.SMOOTH));
        }
        output.add(new CameraKeyframe(metadata.durationMillis(),
                evaluator.evaluate(curve, metadata.durationMillis(), 0.0D), CameraInterpolation.SMOOTH));
        return List.copyOf(output);
    }
}
