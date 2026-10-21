package io.github.kxyr0.echodialogue.forge.resource;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import java.util.List;
import java.util.Objects;

/**
 * Resource discovery output.
 */
public record DialogueResourceReadResult(
        List<DialogueResourceText> resources,
        List<DialogueDiagnostic> diagnostics
) {
    public DialogueResourceReadResult {
        resources = List.copyOf(Objects.requireNonNull(resources, "resources"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }
}
