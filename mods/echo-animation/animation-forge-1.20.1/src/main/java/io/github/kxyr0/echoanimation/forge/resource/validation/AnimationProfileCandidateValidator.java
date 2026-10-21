package io.github.kxyr0.echoanimation.forge.resource.validation;

import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echoanimation.api.profile.AnimationProfileId;
import io.github.kxyr0.echoanimation.api.profile.definition.AnimationProfileDefinition;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates animation profile resources against the loaded body rig catalog.
 */
public final class AnimationProfileCandidateValidator {
    public void validate(
            List<AnimationRigDefinition> rigs,
            List<AnimationProfileDefinition> profiles,
            List<AnimationDiagnostic> diagnostics
    ) {
        Set<AnimationProfileId> seen = new HashSet<>();
        Set<AnimationRigId> rigIds = rigs.stream().map(AnimationRigDefinition::id).collect(Collectors.toSet());
        for (AnimationProfileDefinition profile : profiles) {
            if (!seen.add(profile.id())) {
                diagnostics.add(error("Duplicate animation profile id: " + profile.id()));
            }
            if (!rigIds.contains(profile.rigId())) {
                diagnostics.add(error("Profile references missing body rig: "
                        + profile.id() + " -> " + profile.rigId()));
            }
        }
    }

    private AnimationDiagnostic error(String message) {
        return new AnimationDiagnostic("EANIM-FORGE-PROFILE", DiagnosticSeverity.ERROR, message);
    }
}
