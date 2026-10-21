package io.github.kxyr0.echodialogue.forge.catalog;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable server-side holder of the latest known-good immutable catalog.
 */
public final class DialogueCatalogState {
    private CompiledDialogueCatalogSnapshot current = CompiledDialogueCatalogSnapshot.empty();

    public CompiledDialogueCatalogSnapshot current() {
        return current;
    }

    public CatalogInstallResult install(DialogueCatalogCandidate candidate) {
        List<DialogueDiagnostic> diagnostics = new ArrayList<>(candidate.diagnostics());
        Map<DialogueId, CompiledDialogue> next = new LinkedHashMap<>();
        Map<String, DialogueId> sourceDialogueIds = new LinkedHashMap<>();
        Map<DialogueId, List<String>> sourcesByDialogueId = sourcesByDialogueId(candidate);
        for (CompiledDialogue dialogue : candidate.dialogues()) {
            if (next.putIfAbsent(dialogue.id(), dialogue) != null) {
                diagnostics.add(duplicateDialogueId(dialogue.id(), sourcesByDialogueId));
            }
        }
        if (diagnostics.stream().anyMatch(DialogueDiagnostic::isError)) {
            diagnostics.add(DialogueDiagnostic.error("EDLG-FORGE-002",
                    DiagnosticCategory.RELOAD_REJECTED,
                    "Reload диалогов отклонён; сохранён предыдущий каталог."));
            return new CatalogInstallResult(false, current, diagnostics);
        }
        candidate.sourceDialogueIds().forEach((sourceId, dialogueId) -> {
            if (next.containsKey(dialogueId)) {
                sourceDialogueIds.put(sourceId, dialogueId);
            }
        });
        current = new CompiledDialogueCatalogSnapshot(current.revision() + 1, next, sourceDialogueIds);
        diagnostics.add(DialogueDiagnostic.info("EDLG-FORGE-003",
                DiagnosticCategory.RELOAD_ACCEPTED, "Reload диалогов принят: " + current.dialogueCount()));
        return new CatalogInstallResult(true, current, diagnostics);
    }

    private Map<DialogueId, List<String>> sourcesByDialogueId(DialogueCatalogCandidate candidate) {
        Map<DialogueId, List<String>> values = new LinkedHashMap<>();
        candidate.sourceDialogueIds().forEach((sourceId, dialogueId) ->
                values.computeIfAbsent(dialogueId, ignored -> new ArrayList<>()).add(sourceId));
        return values;
    }

    private DialogueDiagnostic duplicateDialogueId(
            DialogueId dialogueId,
            Map<DialogueId, List<String>> sourcesByDialogueId) {
        List<String> sources = sourcesByDialogueId.getOrDefault(dialogueId, List.of());
        String source = sources.isEmpty() ? "" : sources.get(sources.size() - 1);
        String message = "DialogueId объявлен повторно при reload: " + dialogueId
                + duplicateSources(sources);
        return new DialogueDiagnostic(
                "ECHO_SCRIPT_RESOURCE_ID_COLLISION",
                DiagnosticSeverity.ERROR,
                DiagnosticCategory.DUPLICATE_DIALOGUE_ID,
                message,
                source.isBlank() ? DiagnosticSubject.none() : DiagnosticSubject.source(source));
    }

    private String duplicateSources(List<String> sources) {
        if (sources.size() < 2) {
            return "";
        }
        return ". Источники: " + String.join(", ", sources);
    }
}
