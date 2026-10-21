package io.github.kxyr0.echoik.core.solver;

import io.github.kxyr0.echoik.core.math.Quat;
import io.github.kxyr0.echoik.core.math.Vec3;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolved IK target for one two-bone chain.
 */
public record IkTarget(
        String rootBoneId,
        String midBoneId,
        String endBoneId,
        Vec3 targetPosition,
        Optional<Quat> targetRotation,
        Optional<Vec3> poleTarget,
        double weight,
        double rotationWeight,
        double positionWeight
) {
    public IkTarget {
        rootBoneId = requireId(rootBoneId, "rootBoneId");
        midBoneId = requireId(midBoneId, "midBoneId");
        endBoneId = requireId(endBoneId, "endBoneId");
        targetPosition = Objects.requireNonNull(targetPosition, "targetPosition");
        targetRotation = Objects.requireNonNull(targetRotation, "targetRotation");
        poleTarget = Objects.requireNonNull(poleTarget, "poleTarget");
        weight = requireWeight(weight, "weight");
        rotationWeight = requireWeight(rotationWeight, "rotationWeight");
        positionWeight = requireWeight(positionWeight, "positionWeight");
    }

    private static String requireId(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return checked;
    }

    private static double requireWeight(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0D || value > 1.0D) {
            throw new IllegalArgumentException(name + " must be in range 0..1.");
        }
        return value;
    }
}
