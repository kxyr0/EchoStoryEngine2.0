package io.github.kxyr0.echoanimation.core.playback.curve;

import io.github.kxyr0.echoanimation.api.clip.definition.AnimationKeyframeDefinition;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.InterpolationKind;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

final class CurveScalar {
    private CurveScalar() {}

    record Samples( List<AnimationKeyframeDefinition> frames,
            Function<AnimationKeyframeDefinition, Optional<Vec3>> getter, Vec3 fallback, boolean angle) {}

    private record Endpoints(double left, double right, double fallbackSlope) {}

    private record Tangents(double left, double right, double leftSlope, double rightSlope) {}

    static double cubicComponent( Samples samples, int leftIndex, int rightIndex, double alpha, Axis axis) {
        var left = samples.frames().get(leftIndex);
        var right = samples.frames().get(rightIndex);
        double leftValue = component(value(samples.getter().apply(left), samples.fallback()), axis);
        double rightValue = component(value(samples.getter().apply(right), samples.fallback()), axis);
        if (samples.angle()) {
            rightValue = unwrapNear(leftValue, rightValue);
        }
        double span = right.progress() - left.progress();
        double linearSlope = span == 0.0D ? 0.0D : (rightValue - leftValue) / span;
        var endpoints = new Endpoints(leftValue, rightValue, linearSlope);
        double leftSlope = slope(samples, Math.max(0, leftIndex - 1), rightIndex, axis, endpoints);
        double rightSlope = slope( samples, leftIndex, Math.min(samples.frames().size() - 1, rightIndex + 1), axis,
                        endpoints);
        return hermite(new Tangents(leftValue, rightValue, leftSlope, rightSlope), span, alpha);
    }

    static double alpha(double left, double right, double progress, InterpolationKind kind) {
        double span = right - left;
        double raw = span == 0.0D ? 0.0D : (progress - left) / span;
        double clamped = Math.max(0.0D, Math.min(1.0D, raw));
        return kind == InterpolationKind.SMOOTH ? clamped * clamped * (3.0D - 2.0D * clamped) : clamped;
    }

    static double angle(double left, double right, double alpha) {
        return left + shortestDelta(left, right) * alpha;
    }

    private static double slope( Samples samples, int fromIndex, int toIndex, Axis axis, Endpoints endpoints) {
        if (fromIndex == toIndex) {
            return endpoints.fallbackSlope();
        }
        var from = samples.frames().get(fromIndex);
        var to = samples.frames().get(toIndex);
        double fromValue = component(value(samples.getter().apply(from), samples.fallback()), axis);
        double toValue = component(value(samples.getter().apply(to), samples.fallback()), axis);
        if (samples.angle()) {
            fromValue = unwrapNear(endpoints.left(), fromValue);
            toValue = unwrapNear(endpoints.right(), toValue);
        }
        double span = to.progress() - from.progress();
        return span == 0.0D ? endpoints.fallbackSlope() : (toValue - fromValue) / span;
    }

    private static double hermite(Tangents tangents, double span, double alpha) {
        double alpha2 = alpha * alpha;
        double alpha3 = alpha2 * alpha;
        double h00 = 2.0D * alpha3 - 3.0D * alpha2 + 1.0D;
        double h10 = alpha3 - 2.0D * alpha2 + alpha;
        double h01 = -2.0D * alpha3 + 3.0D * alpha2;
        double h11 = alpha3 - alpha2;
        return h00 * tangents.left() + h10 * span * tangents.leftSlope() + h01 * tangents.right()
                + h11 * span * tangents.rightSlope();
    }

    private static Vec3 value(Optional<Vec3> value, Vec3 fallback) {
        return value.orElse(fallback);
    }

    private static double unwrapNear(double anchor, double value) {
        return anchor + shortestDelta(anchor, value);
    }

    private static double shortestDelta(double left, double right) {
        return ((right - left + 540.0D) % 360.0D) - 180.0D;
    }

    private static double component(Vec3 value, Axis axis) {
        return switch (axis) {
            case X -> value.x();
            case Y -> value.y();
            case Z -> value.z();
        };
    }

    enum Axis {
        X, Y, Z
    }
}
