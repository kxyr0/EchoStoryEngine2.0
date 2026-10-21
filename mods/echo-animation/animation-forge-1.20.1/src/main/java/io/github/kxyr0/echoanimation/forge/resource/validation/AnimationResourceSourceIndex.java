package io.github.kxyr0.echoanimation.forge.resource.validation;

import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.echoscript.diagnostic.AnimationDiagnosticMessages;
import io.github.kxyr0.echoanimation.forge.resource.AnimationResourceText;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Finds the authoring file most likely responsible for catalog diagnostics.
 */
public final class AnimationResourceSourceIndex {
    private final List<AnimationResourceText> resources;

    public AnimationResourceSourceIndex(List<AnimationResourceText> resources) {
        this.resources = List.copyOf(Objects.requireNonNull(resources, "resources"));
    }

    public List<AnimationDiagnostic> localize(List<AnimationDiagnostic> diagnostics) {
        return diagnostics.stream().map(this::localize).toList();
    }

    private AnimationDiagnostic localize(AnimationDiagnostic diagnostic) {
        if (AnimationDiagnosticMessages.hasSource(diagnostic.message())) {
            return diagnostic;
        }
        return resourceFor(diagnostic.message())
                .map(resource -> AnimationDiagnosticMessages.source(diagnostic, resource.sourceId(), resource.text()))
                .orElseGet(() -> AnimationDiagnosticMessages.localized(diagnostic));
    }

    private Optional<AnimationResourceText> resourceFor(String message) {
        String needle = AnimationDiagnosticMessages.sourceNeedle(message);
        if (needle.isEmpty()) {
            return Optional.empty();
        }
        return resources.stream()
                .filter(resource -> resource.sourceId().contains(needle) || resource.text().contains(needle))
                .findFirst();
    }
}
