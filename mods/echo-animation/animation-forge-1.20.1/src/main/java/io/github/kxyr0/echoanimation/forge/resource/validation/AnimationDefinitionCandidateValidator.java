package io.github.kxyr0.echoanimation.forge.resource.validation;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.definition.validation.AnimationDefinitionValidator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Validates high-level animation definitions inside one catalog candidate.
 */
public final class AnimationDefinitionCandidateValidator {
    public void validate(
            List<AnimationRigDefinition> rigs,
            List<AnimationClipDefinition> clips,
            List<AnimationDefinition> definitions,
            List<AnimationDiagnostic> diagnostics
    ) {
        requireUniqueAnimationIds(definitions, diagnostics);
        if (definitions.isEmpty()) {
            return;
        }
        AnimationDefinitionValidator validator = new AnimationDefinitionValidator();
        for (AnimationDefinition definition : definitions) {
            try {
                AnimationRigDefinition rig = rigFor(definition, rigs, clips);
                requireOneRig(definition, rig, clips);
                validateOne(validator, rig, clips, definition, diagnostics);
            } catch (IllegalArgumentException exception) {
                diagnostics.add(error(definition.id() + ": " + exception.getMessage()));
            }
        }
    }

    private AnimationRigDefinition rigFor(
            AnimationDefinition definition,
            List<AnimationRigDefinition> rigs,
            List<AnimationClipDefinition> clips
    ) {
        AnimationClipDefinition main = clips.stream()
                .filter(clip -> clip.id().equals(definition.main().clipId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown animation clip: " + definition.main().clipId()));
        if (main.rigId().isEmpty()) {
            if (rigs.size() == 1) {
                return rigs.get(0);
            }
            throw new IllegalArgumentException("Main clip requires an explicit body rig.");
        }
        AnimationRigId rigId = main.rigId().orElseThrow();
        return rigs.stream().filter(rig -> rig.id().equals(rigId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing body rig: " + rigId));
    }

    private void requireOneRig(
            AnimationDefinition definition,
            AnimationRigDefinition rig,
            List<AnimationClipDefinition> clips
    ) {
        Set<AnimationClipId> referenced = new HashSet<>();
        referenced.add(definition.main().clipId());
        definition.entryVariants().forEach(entry -> referenced.add(entry.clipId()));
        definition.exitVariants().forEach(exit -> referenced.add(exit.clipId()));
        Optional<AnimationClipDefinition> mismatch = clips.stream()
                .filter(clip -> referenced.contains(clip.id()))
                .filter(clip -> clip.rigId().isPresent() && !clip.rigId().orElseThrow().equals(rig.id()))
                .findFirst();
        if (mismatch.isPresent()) {
            throw new IllegalArgumentException("Animation clips reference multiple body rigs.");
        }
    }

    private void requireUniqueAnimationIds(
            List<AnimationDefinition> definitions,
            List<AnimationDiagnostic> diagnostics
    ) {
        Set<AnimationId> seen = new HashSet<>();
        for (AnimationDefinition definition : definitions) {
            if (!seen.add(definition.id())) {
                diagnostics.add(error("Duplicate AnimationId: " + definition.id()));
            }
        }
    }

    private void validateOne(
            AnimationDefinitionValidator validator,
            AnimationRigDefinition rig,
            List<AnimationClipDefinition> clips,
            AnimationDefinition definition,
            List<AnimationDiagnostic> diagnostics
    ) {
        try {
            validator.validate(definition, rig, clips);
        } catch (IllegalArgumentException exception) {
            diagnostics.add(error(definition.id() + ": " + exception.getMessage()));
        }
    }

    private AnimationDiagnostic error(String message) {
        return new AnimationDiagnostic("EANIM-FORGE-ANIMATION", DiagnosticSeverity.ERROR, message);
    }
}
