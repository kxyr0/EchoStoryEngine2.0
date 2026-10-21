package io.github.kxyr0.echoanimation.core.transform;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Evaluated world transforms keyed by BoneId.
 */
public record WorldTransform(Map<BoneId, Transform3> transforms) {
    public WorldTransform {
        transforms = Map.copyOf(Objects.requireNonNull(transforms, "transforms"));
    }

    public Optional<Transform3> get(BoneId id) {
        return Optional.ofNullable(transforms.get(id));
    }
}

