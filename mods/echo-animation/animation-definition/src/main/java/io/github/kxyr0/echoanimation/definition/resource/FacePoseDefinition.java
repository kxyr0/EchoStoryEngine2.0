package io.github.kxyr0.echoanimation.definition.resource;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceControlId;
import io.github.kxyr0.echoanimation.api.id.FacePoseId;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable pose delta definition.
 */
public record FacePoseDefinition(
        FacePoseId id,
        FaceRigId rigId,
        Map<BoneId, Transform3> boneDeltas,
        Map<FaceControlId, Double> controlDeltas,
        String semanticFingerprint
) {
    public FacePoseDefinition {
        id = Objects.requireNonNull(id, "id");
        rigId = Objects.requireNonNull(rigId, "rigId");
        boneDeltas = Map.copyOf(Objects.requireNonNull(boneDeltas, "boneDeltas"));
        controlDeltas = Map.copyOf(Objects.requireNonNull(controlDeltas, "controlDeltas"));
        semanticFingerprint = Objects.requireNonNull(semanticFingerprint, "semanticFingerprint");
        controlDeltas.values().forEach(FacePoseDefinition::requireNormalized);
    }

    private static void requireNormalized(double value) {
        if (!Double.isFinite(value) || value < -1.0D || value > 1.0D) {
            throw new IllegalArgumentException("Pose control delta должен быть finite в диапазоне -1..1.");
        }
    }
}

