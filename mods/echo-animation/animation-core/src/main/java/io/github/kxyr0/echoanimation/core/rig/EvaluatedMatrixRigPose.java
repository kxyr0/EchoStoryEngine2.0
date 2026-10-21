package io.github.kxyr0.echoanimation.core.rig;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.Matrix4Transform;
import io.github.kxyr0.echoanimation.api.rig.AnimationAttachmentSocketId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Model-space exact matrices for bones and attachment sockets.
 */
public record EvaluatedMatrixRigPose(
        Map<BoneId, Matrix4Transform> bones,
        Map<AnimationAttachmentSocketId, Matrix4Transform> sockets
) {
    public EvaluatedMatrixRigPose {
        bones = Map.copyOf(Objects.requireNonNull(bones, "bones"));
        sockets = Map.copyOf(Objects.requireNonNull(sockets, "sockets"));
    }

    public Optional<Matrix4Transform> bone(BoneId id) {
        return Optional.ofNullable(bones.get(id));
    }

    public Optional<Matrix4Transform> socket(AnimationAttachmentSocketId id) {
        return Optional.ofNullable(sockets.get(id));
    }
}
