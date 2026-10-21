package io.github.kxyr0.echoanimation.forge.resource;

import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.profile.definition.AnimationProfileDefinition;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.definition.resource.AnimationResourceSet;
import io.github.kxyr0.echoanimation.definition.validation.AnimationClipValidator;
import io.github.kxyr0.echoanimation.definition.resource.FacePoseDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FacialAnimationDefinition;
import io.github.kxyr0.echoanimation.echoscript.AnimationCompileResult;
import io.github.kxyr0.echoanimation.echoscript.EchoScriptAnimationCompiler;
import io.github.kxyr0.echoanimation.forge.resource.validation.AnimationDefinitionCandidateValidator;
import io.github.kxyr0.echoanimation.forge.resource.validation.AnimationProfileCandidateValidator;
import io.github.kxyr0.echoanimation.forge.resource.validation.AnimationResourceSourceIndex;
import io.github.kxyr0.echoanimation.forge.resource.validation.FaceRigClipValidator;
import io.github.kxyr0.echoanimation.forge.resource.compilation.AnimationSegmentResolutionPass;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Compiles discovered text resources into one catalog candidate.
 */
public final class AnimationResourceCandidateCompiler {
    private final EchoScriptAnimationCompiler compiler;
    private final Map<String, CachedAnimationCompile> cache = new HashMap<>();

    public AnimationResourceCandidateCompiler(EchoScriptAnimationCompiler compiler) {
        this.compiler = Objects.requireNonNull(compiler, "compiler");
    }

    public synchronized AnimationCatalogCandidate compile(List<AnimationResourceText> resources) {
        List<FaceRigDefinition> rigs = new ArrayList<>();
        List<FacePoseDefinition> poses = new ArrayList<>();
        List<FacialAnimationDefinition> animations = new ArrayList<>();
        List<AnimationRigDefinition> bodyRigs = new ArrayList<>();
        List<AnimationClipDefinition> bodyClips = new ArrayList<>();
        List<AnimationDefinition> highLevelAnimations = new ArrayList<>();
        List<AnimationProfileDefinition> profiles = new ArrayList<>();
        List<AnimationDiagnostic> diagnostics = new ArrayList<>();
        AnimationResourceSourceIndex sources = new AnimationResourceSourceIndex(resources);
        retain(resources);
        for (AnimationResourceText resource : resources) {
            CachedAnimationCompile result = cached(resource);
            diagnostics.addAll(result.diagnostics());
            result.faceRig().ifPresent(rigs::add);
            result.facePose().ifPresent(poses::add);
            result.facialAnimation().ifPresent(animations::add);
            result.animationRig().ifPresent(bodyRigs::add);
            result.animationClip().ifPresent(bodyClips::add);
            result.animationDefinition().ifPresent(highLevelAnimations::add);
            result.animationProfile().ifPresent(profiles::add);
        }
        new AnimationSegmentResolutionPass().resolve(bodyClips, highLevelAnimations, diagnostics);
        validateBodyClips(rigs, bodyRigs, bodyClips, diagnostics);
        new AnimationDefinitionCandidateValidator().validate(bodyRigs, bodyClips, highLevelAnimations, diagnostics);
        new AnimationProfileCandidateValidator().validate(bodyRigs, profiles, diagnostics);
        return new AnimationCatalogCandidate(new AnimationResourceSet(rigs, poses, animations, bodyRigs, bodyClips,
                highLevelAnimations, profiles), sources.localize(diagnostics));
    }

    private void validateBodyClips(
            List<FaceRigDefinition> faceRigs,
            List<AnimationRigDefinition> rigs,
            List<AnimationClipDefinition> clips,
            List<AnimationDiagnostic> diagnostics
    ) {
        Map<AnimationRigId, AnimationRigDefinition> byId = bodyRigsById(rigs, diagnostics);
        AnimationClipValidator validator = new AnimationClipValidator();
        FaceRigClipValidator faceValidator = new FaceRigClipValidator();
        for (AnimationClipDefinition clip : clips) {
            if (faceValidator.validateIfFaceRig(clip, faceRigs, diagnostics)) {
                continue;
            }
            Optional<AnimationRigDefinition> rig = rigForClip(clip, rigs, byId, diagnostics);
            rig.ifPresent(definition -> validateClip(validator, definition, clip, diagnostics));
        }
    }

    private Map<AnimationRigId, AnimationRigDefinition> bodyRigsById(
            List<AnimationRigDefinition> rigs,
            List<AnimationDiagnostic> diagnostics
    ) {
        Map<AnimationRigId, AnimationRigDefinition> byId = new HashMap<>();
        for (AnimationRigDefinition rig : rigs) {
            if (byId.putIfAbsent(rig.id(), rig) != null) {
                diagnostics.add(error("EANIM-FORGE-BODY-RIG", "Duplicate body rig id: " + rig.id()));
            }
        }
        return byId;
    }

    private Optional<AnimationRigDefinition> rigForClip(
            AnimationClipDefinition clip,
            List<AnimationRigDefinition> rigs,
            Map<AnimationRigId, AnimationRigDefinition> byId,
            List<AnimationDiagnostic> diagnostics
    ) {
        if (clip.rigId().isPresent()) {
            AnimationRigId rigId = clip.rigId().orElseThrow();
            AnimationRigDefinition rig = byId.get(rigId);
            if (rig == null) {
                diagnostics.add(error("EANIM-FORGE-BODY-CLIP",
                        "Clip references missing body rig: " + clip.id() + " -> " + rigId));
            }
            return Optional.ofNullable(rig);
        }
        return rigs.size() == 1 ? Optional.of(rigs.get(0)) : Optional.empty();
    }

    private void validateClip(
            AnimationClipValidator validator,
            AnimationRigDefinition rig,
            AnimationClipDefinition clip,
            List<AnimationDiagnostic> diagnostics
    ) {
        try {
            validator.validate(rig, clip);
        } catch (IllegalArgumentException exception) {
            diagnostics.add(error("EANIM-FORGE-BODY-CLIP", clip.id() + ": " + exception.getMessage()));
        }
    }

    private AnimationDiagnostic error(String code, String message) {
        return new AnimationDiagnostic(code, DiagnosticSeverity.ERROR, message);
    }

    private CachedAnimationCompile cached(AnimationResourceText resource) {
        CachedAnimationCompile current = cache.get(resource.sourceId());
        if (current != null && current.matches(resource.text())) {
            return current;
        }
        AnimationCompileResult result = compiler.compile(resource.sourceId(), resource.text());
        CachedAnimationCompile next = new CachedAnimationCompile(
                resource.text(),
                result.faceRig(),
                result.facePose(),
                result.facialAnimation(),
                result.animationRig(),
                result.animationClip(),
                result.animationDefinition(),
                result.animationProfile(),
                result.diagnostics());
        cache.put(resource.sourceId(), next);
        return next;
    }

    private void retain(List<AnimationResourceText> resources) {
        Set<String> sourceIds = new HashSet<>();
        for (AnimationResourceText resource : resources) {
            sourceIds.add(resource.sourceId());
        }
        cache.keySet().retainAll(sourceIds);
    }

    private record CachedAnimationCompile(
            String sourceText,
            Optional<FaceRigDefinition> faceRig,
            Optional<FacePoseDefinition> facePose,
            Optional<FacialAnimationDefinition> facialAnimation,
            Optional<AnimationRigDefinition> animationRig,
            Optional<AnimationClipDefinition> animationClip,
            Optional<AnimationDefinition> animationDefinition,
            Optional<AnimationProfileDefinition> animationProfile,
            List<AnimationDiagnostic> diagnostics) {
        private CachedAnimationCompile {
            sourceText = Objects.requireNonNull(sourceText, "sourceText");
            faceRig = Objects.requireNonNull(faceRig, "faceRig");
            facePose = Objects.requireNonNull(facePose, "facePose");
            facialAnimation = Objects.requireNonNull(facialAnimation, "facialAnimation");
            animationRig = Objects.requireNonNull(animationRig, "animationRig");
            animationClip = Objects.requireNonNull(animationClip, "animationClip");
            animationDefinition = Objects.requireNonNull(animationDefinition, "animationDefinition");
            animationProfile = Objects.requireNonNull(animationProfile, "animationProfile");
            diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        }

        private boolean matches(String nextSourceText) {
            return sourceText.equals(nextSourceText);
        }
    }
}
