package io.github.kxyr0.echoik.forge.runtime;

import io.github.kxyr0.echoik.core.math.Vec3;
import java.util.Objects;
import java.util.Optional;

/**
 * Runtime-only IK constraint with independent smooth weight blending.
 */
public record IkConstraint(
        String subjectId,
        Vec3 targetPosition,
        Optional<Vec3> poleTarget,
        double currentWeight,
        double startWeight,
        double targetWeight,
        int blendDurationTicks,
        int blendElapsedTicks,
        boolean clearWhenZero
) {
    private static final double EPSILON = 1.0E-4D;

    public IkConstraint {
        subjectId = require(subjectId);
        targetPosition = Objects.requireNonNull(targetPosition, "targetPosition");
        poleTarget = Objects.requireNonNull(poleTarget, "poleTarget");
        currentWeight = weight(currentWeight);
        startWeight = weight(startWeight);
        targetWeight = weight(targetWeight);
        if (blendDurationTicks < 0 || blendElapsedTicks < 0) {
            throw new IllegalArgumentException("blend ticks must not be negative.");
        }
    }

    public static IkConstraint blendTo(String subjectId, Vec3 target, Optional<Vec3> pole,
            double currentWeight, double targetWeight, boolean clearWhenZero, int durationTicks) {
        return new IkConstraint(subjectId, target, pole, currentWeight, currentWeight, targetWeight,
                Math.max(0, durationTicks), 0, clearWhenZero);
    }

    public IkConstraint tick() {
        if (blendDurationTicks == 0 || blendElapsedTicks >= blendDurationTicks) {
            return new IkConstraint(subjectId, targetPosition, poleTarget, targetWeight, targetWeight,
                    targetWeight, blendDurationTicks, blendDurationTicks, clearWhenZero);
        }
        int elapsed = blendElapsedTicks + 1;
        double t = smoothstep((double) elapsed / (double) blendDurationTicks);
        double weight = startWeight + (targetWeight - startWeight) * t;
        return new IkConstraint(subjectId, targetPosition, poleTarget, weight, startWeight, targetWeight,
                blendDurationTicks, elapsed, clearWhenZero);
    }

    public boolean finishedClear() {
        return clearWhenZero && blendElapsedTicks >= blendDurationTicks && currentWeight <= EPSILON;
    }

    private static double smoothstep(double value) {
        double t = Math.max(0.0D, Math.min(1.0D, value));
        return t * t * (3.0D - 2.0D * t);
    }

    private static double weight(double value) {
        if (!Double.isFinite(value) || value < 0.0D || value > 1.0D) {
            throw new IllegalArgumentException("weight must be in range 0..1.");
        }
        return value;
    }

    private static String require(String value) {
        String checked = Objects.requireNonNull(value, "subjectId").trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException("subjectId must not be blank.");
        }
        return checked;
    }
}
