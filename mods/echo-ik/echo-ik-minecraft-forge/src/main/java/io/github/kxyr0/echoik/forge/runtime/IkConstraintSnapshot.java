package io.github.kxyr0.echoik.forge.runtime;

import io.github.kxyr0.echoik.core.math.Vec3;
import java.util.Objects;
import java.util.Optional;

/**
 * Network-safe runtime IK state for one animation subject.
 */
public record IkConstraintSnapshot(
        String subjectId,
        Optional<Vec3> targetPosition,
        Optional<Vec3> poleTarget,
        double targetWeight,
        boolean clearWhenZero,
        boolean debugEnabled
) {
    public IkConstraintSnapshot {
        subjectId = require(subjectId);
        targetPosition = Objects.requireNonNull(targetPosition, "targetPosition");
        poleTarget = Objects.requireNonNull(poleTarget, "poleTarget");
        if (!Double.isFinite(targetWeight) || targetWeight < 0.0D || targetWeight > 1.0D) {
            throw new IllegalArgumentException("targetWeight must be in range 0..1.");
        }
    }

    private static String require(String value) {
        String checked = Objects.requireNonNull(value, "subjectId").trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException("subjectId must not be blank.");
        }
        return checked;
    }
}
