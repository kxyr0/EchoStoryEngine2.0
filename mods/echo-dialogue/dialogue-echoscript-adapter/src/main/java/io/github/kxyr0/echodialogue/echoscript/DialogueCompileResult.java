package io.github.kxyr0.echodialogue.echoscript;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of parsing and semantically binding one DialogueResource source.
 */
public record DialogueCompileResult(
        Optional<CompiledDialogue> dialogue,
        List<DialogueDiagnostic> diagnostics
) {
    public DialogueCompileResult {
        dialogue = Objects.requireNonNull(dialogue, "dialogue");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        if (diagnostics.stream().anyMatch(DialogueDiagnostic::isError) && dialogue.isPresent()) {
            throw new IllegalArgumentException("Invalid source must not produce CompiledDialogue.");
        }
    }

    public boolean successful() {
        return dialogue.isPresent() && diagnostics.stream().noneMatch(DialogueDiagnostic::isError);
    }
}
