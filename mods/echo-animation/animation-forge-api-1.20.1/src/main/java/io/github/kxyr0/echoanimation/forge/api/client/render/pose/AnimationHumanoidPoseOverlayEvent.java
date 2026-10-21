package io.github.kxyr0.echoanimation.forge.api.client.render.pose;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraftforge.eventbus.api.Event;

/**
 * Client render hook for non-authored local pose overlays after animation sampling.
 */
public final class AnimationHumanoidPoseOverlayEvent extends Event {
    private final AnimationSubjectId subjectId;
    private final AnimationRigDefinition rig;
    private final Map<BoneId, EulerTransform> localTransforms;
    private final AnimationHumanoidPoseRenderFrame frame;
    private Map<BoneId, EulerTransform> overlayTransforms;

    public AnimationHumanoidPoseOverlayEvent(
            AnimationSubjectId subjectId,
            AnimationRigDefinition rig,
            Map<BoneId, EulerTransform> localTransforms,
            AnimationHumanoidPoseRenderFrame frame
    ) {
        this.subjectId = Objects.requireNonNull(subjectId, "subjectId");
        this.rig = Objects.requireNonNull(rig, "rig");
        this.localTransforms = Map.copyOf(Objects.requireNonNull(localTransforms, "localTransforms"));
        this.frame = Objects.requireNonNull(frame, "frame");
    }

    public AnimationSubjectId subjectId() {
        return subjectId;
    }

    public AnimationRigDefinition rig() {
        return rig;
    }

    public Map<BoneId, EulerTransform> localTransforms() {
        return localTransforms;
    }

    public AnimationHumanoidPoseRenderFrame frame() {
        return frame;
    }

    public void applyOverlay(Map<BoneId, EulerTransform> value) {
        overlayTransforms = Map.copyOf(Objects.requireNonNull(value, "value"));
    }

    public Optional<Map<BoneId, EulerTransform>> overlayTransforms() {
        return Optional.ofNullable(overlayTransforms);
    }
}
