package io.github.kxyr0.echoanimation.core.rig;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.rig.AnimationAttachmentSocketId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Model-space bone and socket transforms for rendering.
 */
public record EvaluatedRigPose(
        Map<BoneId, EulerTransform> bones,
        Map<AnimationAttachmentSocketId, EulerTransform> sockets
) {
    public EvaluatedRigPose {
        bones = Map.copyOf(Objects.requireNonNull(bones, "bones"));
        sockets = Map.copyOf(Objects.requireNonNull(sockets, "sockets"));
    }

    public Optional<EulerTransform> bone(BoneId id) {
        return Optional.ofNullable(bones.get(id));
    }

    public Optional<EulerTransform> socket(AnimationAttachmentSocketId id) {
        return Optional.ofNullable(sockets.get(id));
    }
}
