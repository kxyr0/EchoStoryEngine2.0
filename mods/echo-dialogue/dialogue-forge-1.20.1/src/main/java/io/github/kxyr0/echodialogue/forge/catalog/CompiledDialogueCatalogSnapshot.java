package io.github.kxyr0.echodialogue.forge.catalog;

import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable catalog visible to new server sessions.
 */
public record CompiledDialogueCatalogSnapshot(
        int revision,
        Map<DialogueId, CompiledDialogue> dialogues,
        Map<String, DialogueId> sourceDialogueIds
) {
    public CompiledDialogueCatalogSnapshot(int revision, Map<DialogueId, CompiledDialogue> dialogues) {
        this(revision, dialogues, Map.of());
    }

    public CompiledDialogueCatalogSnapshot {
        if (revision < 0) {
            throw new IllegalArgumentException("Catalog revision must not be negative.");
        }
        dialogues = Map.copyOf(Objects.requireNonNull(dialogues, "dialogues"));
        sourceDialogueIds = Map.copyOf(Objects.requireNonNull(sourceDialogueIds, "sourceDialogueIds"));
    }

    public static CompiledDialogueCatalogSnapshot empty() {
        return new CompiledDialogueCatalogSnapshot(0, Map.of(), Map.of());
    }

    public Optional<CompiledDialogue> find(DialogueId id) {
        return Optional.ofNullable(dialogues.get(id));
    }

    public Optional<DialogueId> findBySourceId(String sourceId) {
        return Optional.ofNullable(sourceDialogueIds.get(sourceId));
    }

    public int dialogueCount() {
        return dialogues.size();
    }

    public List<String> sortedIds() {
        return dialogues.keySet().stream().map(DialogueId::toString).sorted(Comparator.naturalOrder()).toList();
    }

    public List<String> sortedSourceIds() {
        return sourceDialogueIds.keySet().stream().sorted(Comparator.naturalOrder()).toList();
    }
}
