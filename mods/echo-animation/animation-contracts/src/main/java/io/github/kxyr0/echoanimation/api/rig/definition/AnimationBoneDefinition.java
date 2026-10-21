package io.github.kxyr0.echoanimation.api.rig.definition;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable local bone definition for 3-axis body rigs.
 */
public record AnimationBoneDefinition(
        BoneId id,
        Optional<BoneId> parentId,
        EulerTransform defaultLocalTransform,
        Optional<String> geometryBinding
) {
    public AnimationBoneDefinition {
        id = Objects.requireNonNull(id, "id");
        parentId = Objects.requireNonNull(parentId, "parentId");
        defaultLocalTransform = Objects.requireNonNull(defaultLocalTransform, "defaultLocalTransform");
        geometryBinding = Objects.requireNonNull(geometryBinding, "geometryBinding");
        if (parentId.filter(id::equals).isPresent()) {
            throw new IllegalArgumentException("Bone cannot parent itself: " + id);
        }
    }

    public static AnimationBoneDefinition root(BoneId id) {
        return new AnimationBoneDefinition(id, Optional.empty(), EulerTransform.IDENTITY, Optional.empty());
    }
}
