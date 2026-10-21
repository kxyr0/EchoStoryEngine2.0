package io.github.kxyr0.echoanimation.forge.client.portrait;

import io.github.kxyr0.echoanimation.api.math.Vec2;
import io.github.kxyr0.echoanimation.definition.resource.FaceControlDefinition;

/**
 * Optional portrait-only idle blink overrides.
 */
public record PortraitBlinkConfig(
        Double upperTravel,
        Double lowerTravel,
        Double gazeX,
        Double gazeY,
        Boolean idle,
        Integer periodTicks,
        Integer closeTicks,
        Integer holdTicks,
        Integer openTicks
) {
    private static final PortraitBlinkConfig IDENTITY =
            new PortraitBlinkConfig(null, null, null, null, null, null, null, null, null);

    public PortraitBlinkConfig {
        requireFinite(upperTravel, "upperTravel");
        requireFinite(lowerTravel, "lowerTravel");
        requireFinite(gazeX, "gazeX");
        requireFinite(gazeY, "gazeY");
        requirePositive(periodTicks, "periodTicks");
        requirePositive(closeTicks, "closeTicks");
        requireNonNegative(holdTicks, "holdTicks");
        requirePositive(openTicks, "openTicks");
    }

    public static PortraitBlinkConfig identity() {
        return IDENTITY;
    }

    public FaceControlDefinition apply(FaceControlDefinition base) {
        double gazeMaxX = authoredMilli(gazeX, base.gazeMax().x());
        double gazeMaxY = authoredMilli(gazeY, base.gazeMax().y());
        return new FaceControlDefinition(
                base.id(),
                authoredMilli(upperTravel, base.blinkUpperTravel()),
                authoredMilli(lowerTravel, base.blinkLowerTravel()),
                new Vec2(-gazeMaxX, -gazeMaxY),
                new Vec2(gazeMaxX, gazeMaxY),
                base.blinkUpperBone(),
                base.blinkLowerBone(),
                idle == null ? base.idleBlink() : idle.booleanValue(),
                periodTicks == null ? base.blinkPeriodTicks() : periodTicks.intValue(),
                closeTicks == null ? base.blinkCloseTicks() : closeTicks.intValue(),
                holdTicks == null ? base.blinkHoldTicks() : holdTicks.intValue(),
                openTicks == null ? base.blinkOpenTicks() : openTicks.intValue());
    }

    private double authoredMilli(Double value, double fallback) {
        return value == null ? fallback : value.doubleValue() / 1000.0D;
    }

    private static void requireFinite(Double value, String name) {
        if (value != null && !Double.isFinite(value.doubleValue())) {
            throw new IllegalArgumentException(name + " должен быть finite.");
        }
    }

    private static void requirePositive(Integer value, String name) {
        if (value != null && value.intValue() <= 0) {
            throw new IllegalArgumentException(name + " должен быть положительным.");
        }
    }

    private static void requireNonNegative(Integer value, String name) {
        if (value != null && value.intValue() < 0) {
            throw new IllegalArgumentException(name + " должен быть неотрицательным.");
        }
    }
}
