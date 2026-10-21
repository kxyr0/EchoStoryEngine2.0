package io.github.kxyr0.echoanimation.api.math;

import java.util.Objects;

/**
 * Local 3-axis transform for body animation authoring.
 */
public record EulerTransform(Vec3 translation, Vec3 rotationDegrees, Vec3 scale, Vec3 pivot) {
    public static final EulerTransform IDENTITY = new EulerTransform(Vec3.ZERO, Vec3.ZERO, Vec3.ONE, Vec3.ZERO);

    public EulerTransform {
        translation = Objects.requireNonNull(translation, "translation");
        rotationDegrees = Objects.requireNonNull(rotationDegrees, "rotationDegrees");
        scale = Objects.requireNonNull(scale, "scale");
        pivot = Objects.requireNonNull(pivot, "pivot");
        if (scale.x() == 0.0D || scale.y() == 0.0D || scale.z() == 0.0D) {
            throw new IllegalArgumentException("EulerTransform scale components must not be zero.");
        }
    }

    public EulerTransform withTranslation(Vec3 value) {
        return new EulerTransform(value, rotationDegrees, scale, pivot);
    }

    public EulerTransform plusDelta(EulerTransform delta) {
        return new EulerTransform(
                translation.plus(delta.translation()),
                rotationDegrees.plus(delta.rotationDegrees()),
                scale.componentTimes(delta.scale()),
                pivot);
    }

    public Vec3 applyPoint(Vec3 point) {
        Vec3 shifted = point.minus(pivot);
        Vec3 scaled = shifted.componentTimes(scale);
        Vec3 rotated = rotateZ(rotateY(rotateX(scaled, rotationDegrees.x()), rotationDegrees.y()),
                rotationDegrees.z());
        return rotated.plus(pivot).plus(translation);
    }

    private static Vec3 rotateX(Vec3 point, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(point.x(), point.y() * cos - point.z() * sin, point.y() * sin + point.z() * cos);
    }

    private static Vec3 rotateY(Vec3 point, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(point.x() * cos + point.z() * sin, point.y(), -point.x() * sin + point.z() * cos);
    }

    private static Vec3 rotateZ(Vec3 point, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(point.x() * cos - point.y() * sin, point.x() * sin + point.y() * cos, point.z());
    }
}
