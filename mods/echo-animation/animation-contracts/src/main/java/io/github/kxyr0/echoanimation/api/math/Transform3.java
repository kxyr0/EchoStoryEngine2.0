package io.github.kxyr0.echoanimation.api.math;

import java.util.Objects;

/**
 * Local transform with explicit pivot and Z rotation for the A0 face plane.
 */
public record Transform3(Vec3 translation, double rotationZDegrees, Vec3 scale, Vec3 pivot) {
    public static final Transform3 IDENTITY = new Transform3(Vec3.ZERO, 0.0D, Vec3.ONE, Vec3.ZERO);

    public Transform3 {
        translation = Objects.requireNonNull(translation, "translation");
        scale = Objects.requireNonNull(scale, "scale");
        pivot = Objects.requireNonNull(pivot, "pivot");
        if (!Double.isFinite(rotationZDegrees)) {
            throw new IllegalArgumentException("rotationZDegrees должен быть finite.");
        }
    }

    public Vec3 applyPoint(Vec3 point) {
        Vec3 shifted = point.minus(pivot);
        Vec3 scaled = shifted.componentTimes(scale);
        Vec3 rotated = rotateZ(scaled, rotationZDegrees);
        return rotated.plus(pivot).plus(translation);
    }

    public Transform3 withTranslation(Vec3 value) {
        return new Transform3(value, rotationZDegrees, scale, pivot);
    }

    public Transform3 plusDelta(Transform3 delta) {
        return new Transform3(
                translation.plus(delta.translation),
                rotationZDegrees + delta.rotationZDegrees,
                scale.componentTimes(delta.scale),
                pivot);
    }

    public static Vec3 rotateZ(Vec3 point, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(point.x() * cos - point.y() * sin, point.x() * sin + point.y() * cos, point.z());
    }
}

