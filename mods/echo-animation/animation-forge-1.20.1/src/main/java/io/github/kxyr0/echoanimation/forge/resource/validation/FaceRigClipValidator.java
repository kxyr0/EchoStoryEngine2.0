package io.github.kxyr0.echoanimation.forge.resource.validation;

import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationBoneDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigKind;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationSkinVariant;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import io.github.kxyr0.echoanimation.definition.validation.AnimationClipValidator;
import java.util.List;
import java.util.Optional;

/**
 * Lets generic visual clips target authored face rigs.
 */
public final class FaceRigClipValidator {
    private final AnimationClipValidator validator = new AnimationClipValidator();

    public boolean validateIfFaceRig(
            AnimationClipDefinition clip,
            List<FaceRigDefinition> faceRigs,
            List<AnimationDiagnostic> diagnostics
    ) {
        Optional<AnimationRigId> rigId = clip.rigId();
        if (rigId.isEmpty()) {
            return false;
        }
        Optional<FaceRigDefinition> faceRig = faceRigs.stream()
                .filter(rig -> rig.id().value().equals(rigId.orElseThrow().value()))
                .findFirst();
        if (faceRig.isEmpty()) {
            return false;
        }
        try {
            validator.validate(adapt(faceRig.orElseThrow()), clip);
        } catch (IllegalArgumentException exception) {
            diagnostics.add(new AnimationDiagnostic("EANIM-FORGE-FACE-CLIP",
                    DiagnosticSeverity.ERROR, clip.id() + ": " + exception.getMessage()));
        }
        return true;
    }

    private AnimationRigDefinition adapt(FaceRigDefinition faceRig) {
        List<AnimationBoneDefinition> bones = faceRig.hierarchy().bones().stream()
                .map(bone -> new AnimationBoneDefinition(bone.id(), bone.parentId(),
                        euler(bone.localTransform()), Optional.empty()))
                .toList();
        return new AnimationRigDefinition(AnimationRigId.parse(faceRig.id().value()), 1L,
                AnimationRigKind.CUSTOM, AnimationSkinVariant.CLASSIC, bones, List.of(), List.of(),
                faceRig.semanticFingerprint());
    }

    private EulerTransform euler(Transform3 transform) {
        return new EulerTransform(transform.translation(),
                new Vec3(0.0D, 0.0D, transform.rotationZDegrees()), transform.scale(), transform.pivot());
    }
}
