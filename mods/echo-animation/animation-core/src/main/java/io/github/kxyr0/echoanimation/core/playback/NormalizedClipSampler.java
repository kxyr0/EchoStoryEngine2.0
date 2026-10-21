package io.github.kxyr0.echoanimation.core.playback;

import io.github.kxyr0.echoanimation.api.clip.definition.AnimationBoneTrackDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipSegment;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationKeyframeDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationRootMotion;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.math.Matrix4Transform;
import io.github.kxyr0.echoanimation.api.model.AnimationLoopSeam;
import io.github.kxyr0.echoanimation.core.playback.curve.TrackCurveInterpolator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Samples normalized clips without owning state authority.
 */
public final class NormalizedClipSampler {
    public AnimationPose sample(AnimationClipDefinition clip, AnimationPose input, double progress) {
        return sample(clip, input, progress, Optional.empty(), Optional.empty());
    }

    public AnimationPose sample(AnimationClipDefinition clip, AnimationPose input, double progress,
            Optional<String> segmentId, Optional<String> pointId) {
        requireProgress(progress);
        double sourceProgress = sourceProgress(clip, progress, segmentId, pointId);
        Map<BoneId, EulerTransform> result = new LinkedHashMap<>(input.transforms());
        Map<BoneId, Matrix4Transform> matrixResult = new LinkedHashMap<>(input.matrixTransforms());
        for (AnimationBoneTrackDefinition track : clip.tracks()) {
            if (extractedRoot(clip, track)) {
                continue;
            }
            BoneId targetBoneId = compatibilityBoneId(clip, track);
            List<AnimationKeyframeDefinition> frames = segmentFrames(clip, track.keyframes(), segmentId);
            if (track.matrixTrack()) {
                EulerTransform eulerBase = input.transform(targetBoneId).orElse(EulerTransform.IDENTITY);
                Matrix4Transform base = input.matrixTransform(targetBoneId)
                        .orElseGet(() -> Matrix4Transform.fromEuler(eulerBase));
                matrixResult.put(targetBoneId, sampleMatrixTrack(frames, base, clip.loopSeam(), sourceProgress));
                result.remove(targetBoneId);
            } else {
                EulerTransform base = input.transform(targetBoneId).orElse(EulerTransform.IDENTITY);
                result.put(targetBoneId, sampleTrack(frames, base, clip.loopSeam(), sourceProgress));
                matrixResult.remove(targetBoneId);
            }
        }
        return AnimationPose.of(result, matrixResult);
    }

    public Optional<EulerTransform> sampleRootMotion(AnimationClipDefinition clip, double progress) {
        requireProgress(progress);
        if (clip.rootMotion().mode() != AnimationRootMotion.Mode.EXTRACT) {
            return Optional.empty();
        }
        return clip.tracks().stream().filter(track -> track.boneId().equals(clip.rootMotion().boneId()))
                .findFirst().map(track -> sampleTrack(track.keyframes(), EulerTransform.IDENTITY,
                        clip.loopSeam(), progress));
    }

    private boolean extractedRoot(AnimationClipDefinition clip, AnimationBoneTrackDefinition track) {
        return clip.rootMotion().mode() == AnimationRootMotion.Mode.EXTRACT
                && track.boneId().equals(clip.rootMotion().boneId());
    }

    /**
     * Split-torso compatibility for clips authored before JDH Humanoid v2.
     *
     * Schema 1/2 had one whole-torso bone named {@code body}. In v2 the old
     * whole-torso pivot became {@code body_lower}, while {@code body} is the
     * new upper-chest child. Moving the legacy track onto body_lower preserves
     * old animations exactly through hierarchy inheritance. Schema 3+ uses the
     * new explicit body_lower/body meaning and is never remapped.
     */
    private BoneId compatibilityBoneId(AnimationClipDefinition clip, AnimationBoneTrackDefinition track) {
        if (clip.schemaVersion() >= 3
                || clip.rigId().map(id -> !"jdh:humanoid".equals(id.value())).orElse(true)
                || !"body".equals(track.boneId().value())
                || clip.tracks().stream().anyMatch(item -> "body_lower".equals(item.boneId().value()))) {
            return track.boneId();
        }
        return BoneId.parse("body_lower");
    }

    private List<AnimationKeyframeDefinition> segmentFrames(AnimationClipDefinition clip,
            List<AnimationKeyframeDefinition> frames, Optional<String> segmentId) {
        if (segmentId.isEmpty() || frames.size() < 2) {
            return frames;
        }
        AnimationClipSegment segment = clip.segment(segmentId.orElseThrow()).orElseThrow(() ->
                new IllegalArgumentException("Unknown clip segment: " + segmentId.orElseThrow()));
        double start = segment.startTick() / (double) duration(clip);
        double end = segment.endTick() / (double) duration(clip);
        int first = firstAtOrAfter(frames, start);
        int last = lastAtOrBefore(frames, end);
        if (first >= frames.size() || last < first) {
            first = Math.max(0, first - 1);
            last = Math.min(frames.size() - 1, first + 1);
        } else {
            if (frames.get(first).progress() > start && first > 0) {
                first--;
            }
            if (frames.get(last).progress() < end && last + 1 < frames.size()) {
                last++;
            }
        }
        return frames.subList(first, last + 1);
    }

    private int firstAtOrAfter(List<AnimationKeyframeDefinition> frames, double progress) {
        for (int index = 0; index < frames.size(); index++) {
            if (frames.get(index).progress() >= progress) {
                return index;
            }
        }
        return frames.size();
    }

    private int lastAtOrBefore(List<AnimationKeyframeDefinition> frames, double progress) {
        for (int index = frames.size() - 1; index >= 0; index--) {
            if (frames.get(index).progress() <= progress) {
                return index;
            }
        }
        return -1;
    }

    private double sourceProgress(AnimationClipDefinition clip, double localProgress,
            Optional<String> segmentId, Optional<String> pointId) {
        if (pointId.isPresent()) {
            int tick = clip.point(pointId.orElseThrow()).orElseThrow(() ->
                    new IllegalArgumentException("Unknown clip point: " + pointId.orElseThrow())).tick();
            return tick / (double) duration(clip);
        }
        if (segmentId.isPresent()) {
            var segment = clip.segment(segmentId.orElseThrow()).orElseThrow(() ->
                    new IllegalArgumentException("Unknown clip segment: " + segmentId.orElseThrow()));
            double tick = segment.startTick() + localProgress * segment.durationTicks();
            return tick / (double) duration(clip);
        }
        return localProgress;
    }

    private int duration(AnimationClipDefinition clip) {
        if (clip.durationTicks() <= 0) {
            throw new IllegalArgumentException("Clip has no source timeline duration: " + clip.id());
        }
        return clip.durationTicks();
    }

    private Matrix4Transform sampleMatrixTrack(
            List<AnimationKeyframeDefinition> frames,
            Matrix4Transform base,
            AnimationLoopSeam loopSeam,
            double progress
    ) {
        if (frames.isEmpty() || progress <= frames.get(0).progress()) {
            return TrackCurveInterpolator.materializeMatrix(frames.isEmpty() ? null : frames.get(0), base);
        }
        for (int index = 1; index < frames.size(); index++) {
            AnimationKeyframeDefinition next = frames.get(index);
            if (progress <= next.progress()) {
                return TrackCurveInterpolator.interpolateMatrix(frames, index - 1, index, base, progress);
            }
        }
        if (loopSeam != AnimationLoopSeam.HOLD && frames.size() > 1) {
            return TrackCurveInterpolator.interpolateMatrixSeam(frames, base, loopSeam, progress);
        }
        return TrackCurveInterpolator.materializeMatrix(frames.get(frames.size() - 1), base);
    }

    private EulerTransform sampleTrack(
            List<AnimationKeyframeDefinition> frames,
            EulerTransform base,
            AnimationLoopSeam loopSeam,
            double progress
    ) {
        if (frames.isEmpty() || progress <= frames.get(0).progress()) {
            return TrackCurveInterpolator.materialize(frames.isEmpty() ? null : frames.get(0), base);
        }
        for (int index = 1; index < frames.size(); index++) {
            AnimationKeyframeDefinition next = frames.get(index);
            if (progress <= next.progress()) {
                return TrackCurveInterpolator.interpolate(frames, index - 1, index, base, progress);
            }
        }
        if (loopSeam != AnimationLoopSeam.HOLD && frames.size() > 1) {
            return TrackCurveInterpolator.interpolateSeam(frames, base, loopSeam, progress);
        }
        return TrackCurveInterpolator.materialize(frames.get(frames.size() - 1), base);
    }

    private void requireProgress(double progress) {
        if (!Double.isFinite(progress) || progress < 0.0D || progress > 1.0D) {
            throw new IllegalArgumentException("progress must be finite in range 0..1.");
        }
    }
}
