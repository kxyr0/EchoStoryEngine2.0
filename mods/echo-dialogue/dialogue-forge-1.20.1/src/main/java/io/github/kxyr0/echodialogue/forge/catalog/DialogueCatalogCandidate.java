package io.github.kxyr0.echodialogue.forge.catalog;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Candidate catalog compiled during one resource reload.
 */
public record DialogueCatalogCandidate(
        List<CompiledDialogue> dialogues,
        Map<String, DialogueId> sourceDialogueIds,
        List<DialogueDiagnostic> diagnostics
) {
    public DialogueCatalogCandidate(List<CompiledDialogue> dialogues, List<DialogueDiagnostic> diagnostics) {
        this(dialogues, Map.of(), diagnostics);
    }

    public DialogueCatalogCandidate {
        dialogues = List.copyOf(Objects.requireNonNull(dialogues, "dialogues"));
        sourceDialogueIds = Map.copyOf(Objects.requireNonNull(sourceDialogueIds, "sourceDialogueIds"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }
}
