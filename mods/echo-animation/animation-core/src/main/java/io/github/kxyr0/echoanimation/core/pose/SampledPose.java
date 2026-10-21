package io.github.kxyr0.echoanimation.core.pose;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable sampled bone deltas.
 */
public record SampledPose(Map<BoneId, Transform3> transforms) {
    public SampledPose {
        transforms = Map.copyOf(Objects.requireNonNull(transforms, "transforms"));
    }
}

