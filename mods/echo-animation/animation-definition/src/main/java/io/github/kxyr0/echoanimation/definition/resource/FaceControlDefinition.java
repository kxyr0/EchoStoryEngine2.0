package io.github.kxyr0.echoanimation.definition.resource;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceControlId;
import io.github.kxyr0.echoanimation.api.math.Vec2;
import java.util.Objects;

/**
 * Character-specific normalized control calibration.
 */
public record FaceControlDefinition(
        FaceControlId id,
        double blinkUpperTravel,
        double blinkLowerTravel,
        Vec2 gazeMin,
        Vec2 gazeMax,
        BoneId blinkUpperBone,
        BoneId blinkLowerBone,
        boolean idleBlink,
        int blinkPeriodTicks,
        int blinkCloseTicks,
        int blinkHoldTicks,
        int blinkOpenTicks
) {
    public FaceControlDefinition {
        id = Objects.requireNonNull(id, "id");
        gazeMin = Objects.requireNonNull(gazeMin, "gazeMin");
        gazeMax = Objects.requireNonNull(gazeMax, "gazeMax");
        blinkUpperBone = Objects.requireNonNull(blinkUpperBone, "blinkUpperBone");
        blinkLowerBone = Objects.requireNonNull(blinkLowerBone, "blinkLowerBone");
        requireFinite(blinkUpperTravel, "blinkUpperTravel");
        requireFinite(blinkLowerTravel, "blinkLowerTravel");
        requirePositive(blinkPeriodTicks, "blinkPeriodTicks");
        requirePositive(blinkCloseTicks, "blinkCloseTicks");
        requireNonNegative(blinkHoldTicks, "blinkHoldTicks");
        requirePositive(blinkOpenTicks, "blinkOpenTicks");
        if (idleBlink && blinkPeriodTicks <= blinkCloseTicks + blinkHoldTicks + blinkOpenTicks) {
            throw new IllegalArgumentException("blinkPeriodTicks must be greater than active blink ticks.");
        }
    }

    private static void requireFinite(double value, String field) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(field + " must be finite.");
        }
    }

    private static void requirePositive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive.");
        }
    }

    private static void requireNonNegative(int value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must be non-negative.");
        }
    }
}
