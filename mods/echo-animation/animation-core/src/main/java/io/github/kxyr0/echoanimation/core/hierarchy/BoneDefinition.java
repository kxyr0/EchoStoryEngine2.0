package io.github.kxyr0.echoanimation.core.hierarchy;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable local bone definition.
 */
public record BoneDefinition(BoneId id, Optional<BoneId> parentId, Transform3 localTransform) {
    public BoneDefinition {
        id = Objects.requireNonNull(id, "id");
        parentId = Objects.requireNonNull(parentId, "parentId");
        localTransform = Objects.requireNonNull(localTransform, "localTransform");
        if (parentId.filter(id::equals).isPresent()) {
            throw new IllegalArgumentException("Bone не может быть parent самого себя: " + id);
        }
    }

    public static BoneDefinition root(BoneId id) {
        return new BoneDefinition(id, Optional.empty(), Transform3.IDENTITY);
    }
}

