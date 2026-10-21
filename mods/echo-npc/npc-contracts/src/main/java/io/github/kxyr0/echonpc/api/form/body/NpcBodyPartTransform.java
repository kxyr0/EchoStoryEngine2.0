package io.github.kxyr0.echonpc.api.form.body;

import java.util.Objects;

/**
 * Cached transform for one semantic body part.
 */
public record NpcBodyPartTransform(
        NpcBodyPartId partId,
        double scaleX,
        double scaleY,
        double scaleZ,
        double pivotX,
        double pivotY,
        double pivotZ,
        double rotationX,
        double rotationY,
        double rotationZ
) {
    public NpcBodyPartTransform {
        partId = Objects.requireNonNull(partId, "partId");
        boundedScale(scaleX, "scaleX");
        boundedScale(scaleY, "scaleY");
        boundedScale(scaleZ, "scaleZ");
        boundedOffset(pivotX, "pivotX");
        boundedOffset(pivotY, "pivotY");
        boundedOffset(pivotZ, "pivotZ");
        boundedRotation(rotationX, "rotationX");
        boundedRotation(rotationY, "rotationY");
        boundedRotation(rotationZ, "rotationZ");
    }

    public static NpcBodyPartTransform uniform(NpcBodyPartId partId, double scale) {
        return new NpcBodyPartTransform(partId, scale, scale, scale, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static void boundedScale(double value, String name) {
        if (!Double.isFinite(value) || value < 0.05D || value > 4.0D) {
            throw new IllegalArgumentException("Part " + name + " is invalid.");
        }
    }

    private static void boundedOffset(double value, String name) {
        if (!Double.isFinite(value) || Math.abs(value) > 4.0D) {
            throw new IllegalArgumentException("Part " + name + " is invalid.");
        }
    }

    private static void boundedRotation(double value, String name) {
        if (!Double.isFinite(value) || Math.abs(value) > 180.0D) {
            throw new IllegalArgumentException("Part " + name + " is invalid.");
        }
    }
}
