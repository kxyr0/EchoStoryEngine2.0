package io.github.kxyr0.echoanimation.echoscript;

import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.api.profile.definition.AnimationProfileDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FacePoseDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FacialAnimationDefinition;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of syntax parsing followed by Echo Animation semantic binding.
 */
public record AnimationCompileResult(
        Optional<FaceRigDefinition> faceRig,
        Optional<FacePoseDefinition> facePose,
        Optional<FacialAnimationDefinition> facialAnimation,
        Optional<AnimationRigDefinition> animationRig,
        Optional<AnimationClipDefinition> animationClip,
        Optional<AnimationDefinition> animationDefinition,
        Optional<AnimationProfileDefinition> animationProfile,
        List<AnimationDiagnostic> diagnostics
) {
    public AnimationCompileResult(
            Optional<FaceRigDefinition> faceRig,
            Optional<FacePoseDefinition> facePose,
            Optional<FacialAnimationDefinition> facialAnimation,
            List<AnimationDiagnostic> diagnostics
    ) {
        this(faceRig, facePose, facialAnimation, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), diagnostics);
    }

    public AnimationCompileResult {
        faceRig = Objects.requireNonNull(faceRig, "faceRig");
        facePose = Objects.requireNonNull(facePose, "facePose");
        facialAnimation = Objects.requireNonNull(facialAnimation, "facialAnimation");
        animationRig = Objects.requireNonNull(animationRig, "animationRig");
        animationClip = Objects.requireNonNull(animationClip, "animationClip");
        animationDefinition = Objects.requireNonNull(animationDefinition, "animationDefinition");
        animationProfile = Objects.requireNonNull(animationProfile, "animationProfile");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public boolean successful() {
        return diagnostics.stream().noneMatch(AnimationDiagnostic::error)
                && (faceRig.isPresent() || facePose.isPresent() || facialAnimation.isPresent()
                || animationRig.isPresent() || animationClip.isPresent() || animationDefinition.isPresent()
                || animationProfile.isPresent());
    }
}
