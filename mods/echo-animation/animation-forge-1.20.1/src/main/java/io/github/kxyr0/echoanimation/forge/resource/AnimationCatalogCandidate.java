package io.github.kxyr0.echoanimation.forge.resource;

import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.definition.resource.AnimationResourceSet;
import java.util.List;
import java.util.Objects;

/**
 * Candidate catalog produced during resource reload.
 */
public record AnimationCatalogCandidate(AnimationResourceSet resourceSet, List<AnimationDiagnostic> diagnostics) {
    public AnimationCatalogCandidate {
        resourceSet = Objects.requireNonNull(resourceSet, "resourceSet");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public boolean valid() {
        return diagnostics.stream().noneMatch(AnimationDiagnostic::error);
    }
}

