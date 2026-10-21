package io.github.kxyr0.echocutscene.api.camera;

import io.github.kxyr0.echocutscene.api.camera.framing.CameraFramingKeyframe;
import io.github.kxyr0.echocutscene.api.camera.mode.CameraModeKeyframe;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetKeyframe;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record CameraTrackSnapshot(
        CameraTrackMetadata metadata,
        List<CameraKeyframe> keyframes,
        List<CameraModeKeyframe> modeKeyframes,
        List<CameraTargetKeyframe> targetKeyframes,
        List<CameraFramingKeyframe> framingKeyframes
) {
    public CameraTrackSnapshot(CameraTrackMetadata metadata, List<CameraKeyframe> keyframes) {
        this(metadata, keyframes, List.of(), List.of(), List.of());
    }

    public CameraTrackSnapshot {
        metadata = Objects.requireNonNull(metadata, "metadata");
        keyframes = List.copyOf(Objects.requireNonNull(keyframes, "keyframes"));
        modeKeyframes = List.copyOf(Objects.requireNonNull(modeKeyframes, "modeKeyframes"));
        targetKeyframes = List.copyOf(Objects.requireNonNull(targetKeyframes, "targetKeyframes"));
        framingKeyframes = List.copyOf(Objects.requireNonNull(framingKeyframes, "framingKeyframes"));
        if (keyframes.size() < 2) {
            throw new IllegalArgumentException("Camera track requires at least two keyframes.");
        }
        requireSorted(keyframes, "Camera keyframes");
        requireSorted(modeKeyframes, "Camera mode keyframes");
        requireSorted(targetKeyframes, "Camera target keyframes");
        requireSorted(framingKeyframes, "Camera framing keyframes");
        if (keyframes.get(keyframes.size() - 1).timeMillis() > metadata.durationMillis()) {
            throw new IllegalArgumentException("Camera keyframe exceeds track duration.");
        }
        requireWithinDuration(modeKeyframes, metadata.durationMillis(), "mode");
        requireWithinDuration(targetKeyframes, metadata.durationMillis(), "target");
        requireWithinDuration(framingKeyframes, metadata.durationMillis(), "framing");
    }

    private static void requireWithinDuration(List<?> frames, long duration, String name) {
        if (!frames.isEmpty() && time(frames.get(frames.size() - 1)) > duration) {
            throw new IllegalArgumentException("Camera " + name + " keyframe exceeds track duration.");
        }
    }

    private static void requireSorted(List<?> frames, String name) {
        if (!frames.stream().sorted(Comparator.comparingLong(CameraTrackSnapshot::time)).toList().equals(frames)) {
            throw new IllegalArgumentException(name + " must be sorted by timeMillis.");
        }
    }

    private static long time(Object frame) {
        if (frame instanceof CameraKeyframe keyframe) {
            return keyframe.timeMillis();
        }
        if (frame instanceof CameraModeKeyframe keyframe) {
            return keyframe.timeMillis();
        }
        if (frame instanceof CameraTargetKeyframe keyframe) {
            return keyframe.timeMillis();
        }
        return ((CameraFramingKeyframe) frame).timeMillis();
    }
}
