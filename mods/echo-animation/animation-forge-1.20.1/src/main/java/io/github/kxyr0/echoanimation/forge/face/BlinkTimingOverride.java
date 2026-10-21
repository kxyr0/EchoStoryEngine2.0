package io.github.kxyr0.echoanimation.forge.face;

import io.github.kxyr0.echoanimation.definition.resource.FaceControlDefinition;

public record BlinkTimingOverride(
        int periodTicks,
        int closeTicks,
        int holdTicks,
        int openTicks
) {
    public BlinkTimingOverride {
        requirePositive(periodTicks, "periodTicks");
        requirePositive(closeTicks, "closeTicks");
        requireNonNegative(holdTicks, "holdTicks");
        requirePositive(openTicks, "openTicks");
        if (periodTicks <= closeTicks + holdTicks + openTicks) {
            throw new IllegalArgumentException("periodTicks must be greater than closeTicks + holdTicks + openTicks.");
        }
    }

    public FaceControlDefinition apply(FaceControlDefinition control) {
        return new FaceControlDefinition(control.id(), control.blinkUpperTravel(), control.blinkLowerTravel(),
                control.gazeMin(), control.gazeMax(), control.blinkUpperBone(), control.blinkLowerBone(),
                control.idleBlink(), periodTicks, closeTicks, holdTicks, openTicks);
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
