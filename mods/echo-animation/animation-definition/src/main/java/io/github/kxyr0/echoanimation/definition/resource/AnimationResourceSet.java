package io.github.kxyr0.echoanimation.definition.resource;

import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.profile.definition.AnimationProfileDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import java.util.List;
import java.util.Objects;

/**
 * Immutable installed resource catalog snapshot.
 */
public record AnimationResourceSet(
        List<FaceRigDefinition> faceRigs,
        List<FacePoseDefinition> facePoses,
        List<FacialAnimationDefinition> facialAnimations,
        List<AnimationRigDefinition> animationRigs,
        List<AnimationClipDefinition> animationClips,
        List<AnimationDefinition> animations,
        List<AnimationProfileDefinition> animationProfiles
) {
    public AnimationResourceSet(
            List<FaceRigDefinition> faceRigs,
            List<FacePoseDefinition> facePoses,
            List<FacialAnimationDefinition> facialAnimations
    ) {
        this(faceRigs, facePoses, facialAnimations, List.of(), List.of(), List.of(), List.of());
    }

    public AnimationResourceSet {
        faceRigs = List.copyOf(Objects.requireNonNull(faceRigs, "faceRigs"));
        facePoses = List.copyOf(Objects.requireNonNull(facePoses, "facePoses"));
        facialAnimations = List.copyOf(Objects.requireNonNull(facialAnimations, "facialAnimations"));
        animationRigs = List.copyOf(Objects.requireNonNull(animationRigs, "animationRigs"));
        animationClips = List.copyOf(Objects.requireNonNull(animationClips, "animationClips"));
        animations = List.copyOf(Objects.requireNonNull(animations, "animations"));
        animationProfiles = List.copyOf(Objects.requireNonNull(animationProfiles, "animationProfiles"));
    }
}
