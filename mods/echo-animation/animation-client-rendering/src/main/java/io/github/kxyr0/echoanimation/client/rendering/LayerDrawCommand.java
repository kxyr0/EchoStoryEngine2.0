package io.github.kxyr0.echoanimation.client.rendering;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceLayerId;
import io.github.kxyr0.echoanimation.api.id.FaceTextureId;
import io.github.kxyr0.echoanimation.api.math.IntRect;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.model.LayerRole;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable draw command produced after layer ordering and visibility filtering.
 */
public record LayerDrawCommand(
        FaceLayerId layerId,
        BoneId boneId,
        FaceTextureId textureId,
        LayerRole role,
        Transform3 placement,
        IntRect atlasRect,
        Optional<IntRect> clipRect
) {
    public LayerDrawCommand {
        layerId = Objects.requireNonNull(layerId, "layerId");
        boneId = Objects.requireNonNull(boneId, "boneId");
        textureId = Objects.requireNonNull(textureId, "textureId");
        role = Objects.requireNonNull(role, "role");
        placement = Objects.requireNonNull(placement, "placement");
        atlasRect = Objects.requireNonNull(atlasRect, "atlasRect");
        clipRect = Objects.requireNonNull(clipRect, "clipRect");
    }
}
