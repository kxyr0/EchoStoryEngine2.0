package io.github.kxyr0.echodialogue.forge.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import io.github.kxyr0.echodialogue.forge.catalog.CompiledDialogueCatalogSnapshot;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class DialoguePlayTargetResolverTest {
    private final DialoguePlayTargetResolver resolver = new DialoguePlayTargetResolver();

    @Test
    void resolvesShortScriptFileNameToDialogueId() {
        DialogueId id = DialogueId.parse("jdh:chapter_1_home_arrival");
        CompiledDialogueCatalogSnapshot catalog = new CompiledDialogueCatalogSnapshot(1, Map.of(id, dialogue(id)),
                Map.of("jdh:echo_dialogue/dialogues/chapter_1/home/home_arrival.dialogue.es", id));

        var resolution = resolver.resolve(catalog, "home_arrival.es");

        assertTrue(resolution.resolved());
        assertEquals(id, resolution.dialogueId());
    }

    @Test
    void resolvesNamespacedDialogueIdWhenCatalogContainsIt() {
        DialogueId id = DialogueId.parse("jdh:father_leaves");
        CompiledDialogueCatalogSnapshot catalog = new CompiledDialogueCatalogSnapshot(1, Map.of(id, dialogue(id)),
                Map.of("jdh:echo_dialogue/dialogues/chapter_1/home/father_leaves.dialogue.es", id));

        var resolution = resolver.resolve(catalog, "jdh:father_leaves");

        assertTrue(resolution.resolved());
        assertEquals(id, resolution.dialogueId());
    }

    @Test
    void resolvesDefaultNamespaceResourceLocationAsShortScriptName() {
        DialogueId id = DialogueId.parse("jdh:father_leaves");
        CompiledDialogueCatalogSnapshot catalog = new CompiledDialogueCatalogSnapshot(1, Map.of(id, dialogue(id)),
                Map.of("jdh:echo_dialogue/dialogues/chapter_1/home/father_leaves.dialogue.es", id));

        var resolution = resolver.resolve(catalog, "minecraft:father_leaves");

        assertTrue(resolution.resolved());
        assertEquals(id, resolution.dialogueId());
    }

    @Test
    void reportsAmbiguousShortFileName() {
        DialogueId first = DialogueId.parse("jdh:first");
        DialogueId second = DialogueId.parse("jdh:second");
        CompiledDialogueCatalogSnapshot catalog = new CompiledDialogueCatalogSnapshot(1,
                Map.of(first, dialogue(first), second, dialogue(second)),
                Map.of(
                        "jdh:echo_dialogue/dialogues/a/home_arrival.dialogue.es", first,
                        "jdh:echo_dialogue/dialogues/b/home_arrival.dialogue.es", second));

        var resolution = resolver.resolve(catalog, "home_arrival.es");

        assertTrue(resolution.ambiguous());
        assertEquals(2, resolution.ambiguousMatches().size());
    }

    private static CompiledDialogue dialogue(DialogueId id) {
        DialogueNodeId nodeId = new DialogueNodeId("start");
        return CompiledDialogue.linear(id, nodeId, List.of(new CompiledDialogueLine(
                nodeId,
                0,
                SpeakerId.parse("sample:father"),
                "Отец",
                "Где ты был?")));
    }
}
