package io.github.kxyr0.echoanimation.api.rig.definition;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.IntRect;
import java.util.Objects;

/**
 * Vanilla-skin cuboid ownership metadata for one visible bone segment.
 */
public record AnimationBoneGeometry(
        String id,
        BoneId ownerBoneId,
        double widthPixels,
        double heightPixels,
        double depthPixels,
        IntRect uv,
        boolean overlay
) {
    public AnimationBoneGeometry {
        id = Objects.requireNonNull(id, "id");
        ownerBoneId = Objects.requireNonNull(ownerBoneId, "ownerBoneId");
        uv = Objects.requireNonNull(uv, "uv");
        if (widthPixels <= 0.0D || heightPixels <= 0.0D || depthPixels <= 0.0D) {
            throw new IllegalArgumentException("Geometry dimensions must be positive.");
        }
    }
}
