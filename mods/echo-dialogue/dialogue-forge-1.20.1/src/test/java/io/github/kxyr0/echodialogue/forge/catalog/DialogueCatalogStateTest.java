package io.github.kxyr0.echodialogue.forge.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import java.util.List;
import org.junit.jupiter.api.Test;

class DialogueCatalogStateTest {
    @Test
    void validCandidateInstallsAtomically() {
        DialogueCatalogState state = new DialogueCatalogState();

        CatalogInstallResult result = state.install(new DialogueCatalogCandidate(
                List.of(dialogue("sample:a")),
                List.of()));

        assertTrue(result.installed());
        assertEquals(1, state.current().revision());
        assertEquals(1, state.current().dialogueCount());
    }

    @Test
    void invalidCandidateRetainsPreviousKnownGoodCatalog() {
        DialogueCatalogState state = new DialogueCatalogState();
        state.install(new DialogueCatalogCandidate(List.of(dialogue("sample:a")), List.of()));

        CatalogInstallResult rejected = state.install(new DialogueCatalogCandidate(
                List.of(dialogue("sample:b"), dialogue("sample:b")), List.of()));

        assertFalse(rejected.installed());
        assertEquals(1, state.current().revision());
        assertTrue(state.current().find(DialogueId.parse("sample:a")).isPresent());
        assertTrue(rejected.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.category() == DiagnosticCategory.DUPLICATE_DIALOGUE_ID));
    }

    private static CompiledDialogue dialogue(String id) {
        DialogueNodeId nodeId = new DialogueNodeId("greeting");
        return CompiledDialogue.linear(DialogueId.parse(id), nodeId, List.of(
                new CompiledDialogueLine(nodeId, 0, SpeakerId.parse("sample:father"), "Отец", "Где ты был?")
        ));
    }
}
