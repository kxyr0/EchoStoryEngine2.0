package io.github.kxyr0.echoanimation.core.hierarchy;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable local transform overrides keyed by BoneId.
 */
public record BonePose(Map<BoneId, Transform3> localTransforms) {
    public BonePose {
        localTransforms = Map.copyOf(Objects.requireNonNull(localTransforms, "localTransforms"));
    }

    public Optional<Transform3> transform(BoneId id) {
        return Optional.ofNullable(localTransforms.get(id));
    }
}

