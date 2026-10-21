package io.github.kxyr0.echoanimation.definition.resource;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceLayerId;
import io.github.kxyr0.echoanimation.api.id.FaceTextureId;
import io.github.kxyr0.echoanimation.api.math.IntRect;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.model.LayerRole;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable face layer binding to one bone and one texture region.
 */
public record FaceLayerDefinition(
        FaceLayerId id,
        BoneId boneId,
        FaceTextureId textureId,
        LayerRole role,
        int drawOrder,
        Transform3 placement,
        IntRect atlasRect,
        Optional<IntRect> clipRect,
        boolean visibleByDefault
) {
    public FaceLayerDefinition {
        id = Objects.requireNonNull(id, "id");
        boneId = Objects.requireNonNull(boneId, "boneId");
        textureId = Objects.requireNonNull(textureId, "textureId");
        role = Objects.requireNonNull(role, "role");
        placement = Objects.requireNonNull(placement, "placement");
        atlasRect = Objects.requireNonNull(atlasRect, "atlasRect");
        clipRect = Objects.requireNonNull(clipRect, "clipRect");
    }
}
