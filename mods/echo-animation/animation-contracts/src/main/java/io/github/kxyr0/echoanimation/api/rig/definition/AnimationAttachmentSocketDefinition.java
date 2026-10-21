package io.github.kxyr0.echoanimation.api.rig.definition;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.rig.AnimationAttachmentSocketId;
import java.util.Objects;

/**
 * Generic transform socket evaluated from an animated parent bone.
 */
public record AnimationAttachmentSocketDefinition(
        AnimationAttachmentSocketId id,
        BoneId parentBoneId,
        EulerTransform localTransform
) {
    public AnimationAttachmentSocketDefinition {
        id = Objects.requireNonNull(id, "id");
        parentBoneId = Objects.requireNonNull(parentBoneId, "parentBoneId");
        localTransform = Objects.requireNonNull(localTransform, "localTransform");
    }
}
