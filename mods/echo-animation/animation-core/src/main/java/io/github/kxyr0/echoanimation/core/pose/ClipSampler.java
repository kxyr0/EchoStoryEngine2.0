package io.github.kxyr0.echoanimation.core.pose;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.AnimationEndBehavior;
import io.github.kxyr0.echoanimation.api.model.InterpolationKind;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Samples facial animation bone tracks without owning actor state.
 */
public final class ClipSampler {
    public SampledPose sample(List<BoneTrack> tracks, double duration, AnimationEndBehavior end, double time) {
        double sampledTime = normalizeTime(duration, end, time);
        Map<BoneId, Transform3> result = new LinkedHashMap<>();
        for (BoneTrack track : tracks) {
            result.put(track.boneId(), sampleTrack(track.keyframes(), sampledTime));
        }
        return new SampledPose(result);
    }

    private double normalizeTime(double duration, AnimationEndBehavior end, double time) {
        if (!Double.isFinite(duration) || duration <= 0.0D || !Double.isFinite(time)) {
            throw new IllegalArgumentException("duration/time должны быть finite.");
        }
        if (time <= duration) {
            return Math.max(0.0D, time);
        }
        return switch (end) {
            case HOLD -> duration;
            case RETURN_TO_BASE -> 0.0D;
            case LOOP -> time % duration;
        };
    }

    private Transform3 sampleTrack(List<Keyframe> frames, double time) {
        if (frames.isEmpty() || time <= frames.get(0).timeSeconds()) {
            return frames.isEmpty() ? Transform3.IDENTITY : frames.get(0).transform();
        }
        for (int index = 1; index < frames.size(); index++) {
            Keyframe next = frames.get(index);
            if (time <= next.timeSeconds()) {
                Keyframe previous = frames.get(index - 1);
                return interpolate(previous, next, time);
            }
        }
        return frames.get(frames.size() - 1).transform();
    }

    private Transform3 interpolate(Keyframe left, Keyframe right, double time) {
        if (left.interpolation() == InterpolationKind.STEP) {
            return left.transform();
        }
        double span = right.timeSeconds() - left.timeSeconds();
        double rawAlpha = span == 0.0D ? 0.0D : (time - left.timeSeconds()) / span;
        double alpha = curveAlpha(left.interpolation(), Math.max(0.0D, Math.min(1.0D, rawAlpha)));
        return lerp(left.transform(), right.transform(), alpha);
    }

    private Transform3 lerp(Transform3 left, Transform3 right, double alpha) {
        return new Transform3(
                lerp(left.translation(), right.translation(), alpha),
                angle(left.rotationZDegrees(), right.rotationZDegrees(), alpha),
                lerp(left.scale(), right.scale(), alpha),
                left.pivot());
    }

    private double curveAlpha(InterpolationKind kind, double alpha) {
        return switch (kind) {
            case SMOOTH -> alpha * alpha * (3.0D - 2.0D * alpha);
            case CUBIC -> alpha < 0.5D ? 4.0D * alpha * alpha * alpha
                    : 1.0D - Math.pow(-2.0D * alpha + 2.0D, 3.0D) / 2.0D;
            default -> alpha;
        };
    }

    private double angle(double left, double right, double alpha) {
        double delta = ((right - left + 540.0D) % 360.0D) - 180.0D;
        return left + delta * alpha;
    }

    private Vec3 lerp(Vec3 left, Vec3 right, double alpha) {
        return new Vec3(
                left.x() + (right.x() - left.x()) * alpha,
                left.y() + (right.y() - left.y()) * alpha,
                left.z() + (right.z() - left.z()) * alpha);
    }
}
