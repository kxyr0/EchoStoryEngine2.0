package io.github.kxyr0.echoanimation.core.playback.curve;

import io.github.kxyr0.echoanimation.api.clip.definition.AnimationKeyframeDefinition;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.math.Matrix4Transform;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.AnimationLoopSeam;
import io.github.kxyr0.echoanimation.api.model.InterpolationKind;

import java.util.List;
import java.util.Optional;

public final class TrackCurveInterpolator {
    private TrackCurveInterpolator() {}

    public static Matrix4Transform materializeMatrix( AnimationKeyframeDefinition frame, Matrix4Transform base) {
        if (frame == null) {
            return base;
        }
        return frame.localMatrix().orElse(base);
    }

    public static Matrix4Transform interpolateMatrix( List<AnimationKeyframeDefinition> frames, int leftIndex,
            int rightIndex, Matrix4Transform base, double progress) {
        AnimationKeyframeDefinition left = frames.get(leftIndex);
        AnimationKeyframeDefinition right = frames.get(rightIndex);
        if (left.interpolation() == InterpolationKind.STEP) {
            return materializeMatrix(left, base);
        }
        double alpha = CurveScalar.alpha( left.progress(), right.progress(), progress, left.interpolation());
        return Matrix4Transform.lerp( materializeMatrix(left, base), materializeMatrix(right, base), alpha);
    }

    public static Matrix4Transform interpolateMatrixSeam( List<AnimationKeyframeDefinition> frames,
            Matrix4Transform base, AnimationLoopSeam seam, double progress) {
        AnimationKeyframeDefinition left = frames.get(frames.size() - 1);
        AnimationKeyframeDefinition right = frames.get(0);
        double alpha = CurveScalar.alpha( left.progress(), 1.0D, progress, seam == AnimationLoopSeam.SMOOTH
                                ? InterpolationKind.SMOOTH : InterpolationKind.LINEAR);
        return Matrix4Transform.lerp( materializeMatrix(left, base), materializeMatrix(right, base), alpha);
    }

    public static EulerTransform materialize( AnimationKeyframeDefinition frame, EulerTransform base) {
        if (frame == null) {
            return base;
        }
        return new EulerTransform( value(frame.translation(), base.translation()),
                value(frame.rotationDegrees(), base.rotationDegrees()), value(frame.scale(), base.scale()),
                base.pivot());
    }

    public static EulerTransform interpolate( List<AnimationKeyframeDefinition> frames, int leftIndex, int rightIndex,
            EulerTransform base, double progress) {
        AnimationKeyframeDefinition left = frames.get(leftIndex);
        AnimationKeyframeDefinition right = frames.get(rightIndex);
        if (left.interpolation() == InterpolationKind.STEP) {
            return materialize(left, base);
        }
        if (left.interpolation() == InterpolationKind.CUBIC) {
            return cubic(frames, leftIndex, rightIndex, base, progress);
        }
        double alpha = CurveScalar.alpha( left.progress(), right.progress(), progress, left.interpolation());
        return lerp(left, right, base, alpha);
    }

    public static EulerTransform interpolateSeam( List<AnimationKeyframeDefinition> frames, EulerTransform base,
            AnimationLoopSeam seam, double progress) {
        AnimationKeyframeDefinition left = frames.get(frames.size() - 1);
        AnimationKeyframeDefinition right = frames.get(0);
        double alpha = CurveScalar.alpha( left.progress(), 1.0D, progress, seam == AnimationLoopSeam.SMOOTH
                                ? InterpolationKind.SMOOTH : InterpolationKind.LINEAR);
        return new EulerTransform( lerp( value(left.translation(), base.translation()),
                        value(right.translation(), base.translation()), alpha), lerpAngles(
                        value(left.rotationDegrees(), base.rotationDegrees()),
                        value(right.rotationDegrees(), base.rotationDegrees()), alpha),
                lerp(value(left.scale(), base.scale()), value(right.scale(), base.scale()), alpha), base.pivot());
    }

    private static EulerTransform lerp( AnimationKeyframeDefinition left, AnimationKeyframeDefinition right,
            EulerTransform base, double alpha) {
        return new EulerTransform( lerp( value(left.translation(), base.translation()),
                        value(right.translation(), base.translation()), alpha), lerpAngles(
                        value(left.rotationDegrees(), base.rotationDegrees()),
                        value(right.rotationDegrees(), base.rotationDegrees()), alpha),
                lerp(value(left.scale(), base.scale()), value(right.scale(), base.scale()), alpha), base.pivot());
    }

    private static EulerTransform cubic( List<AnimationKeyframeDefinition> frames, int leftIndex, int rightIndex,
            EulerTransform base, double progress) {
        AnimationKeyframeDefinition left = frames.get(leftIndex);
        AnimationKeyframeDefinition right = frames.get(rightIndex);
        double alpha = CurveScalar.alpha( left.progress(), right.progress(), progress, InterpolationKind.LINEAR);
        return new EulerTransform( cubicVec( new CurveScalar.Samples( frames, AnimationKeyframeDefinition::translation,
                                base.translation(), false), leftIndex, rightIndex, alpha), cubicVec(
                        new CurveScalar.Samples( frames, AnimationKeyframeDefinition::rotationDegrees,
                                base.rotationDegrees(), true), leftIndex, rightIndex, alpha), cubicVec(
                        new CurveScalar.Samples( frames, AnimationKeyframeDefinition::scale, base.scale(), false),
                        leftIndex, rightIndex, alpha), base.pivot());
    }

    private static Vec3 cubicVec( CurveScalar.Samples samples, int leftIndex, int rightIndex, double alpha) {
        return new Vec3( CurveScalar.cubicComponent( samples, leftIndex, rightIndex, alpha, CurveScalar.Axis.X),
                CurveScalar.cubicComponent( samples, leftIndex, rightIndex, alpha, CurveScalar.Axis.Y),
                CurveScalar.cubicComponent( samples, leftIndex, rightIndex, alpha, CurveScalar.Axis.Z));
    }

    private static Vec3 value(Optional<Vec3> value, Vec3 fallback) {
        return value.orElse(fallback);
    }

    private static Vec3 lerp(Vec3 left, Vec3 right, double alpha) {
        return new Vec3( left.x() + (right.x() - left.x()) * alpha, left.y() + (right.y() - left.y()) * alpha,
                left.z() + (right.z() - left.z()) * alpha);
    }

    private static Vec3 lerpAngles(Vec3 left, Vec3 right, double alpha) {
        return new Vec3( CurveScalar.angle(left.x(), right.x(), alpha), CurveScalar.angle(left.y(), right.y(), alpha),
                CurveScalar.angle(left.z(), right.z(), alpha));
    }
}
