package io.github.kxyr0.echodialogue.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompiledDialogueImmutabilityTest {
    @Test
    void compiledDialogueDefensivelyCopiesLines() {
        List<CompiledDialogueLine> lines = new ArrayList<>();
        lines.add(line("Отец", "Где ты был?"));

        CompiledDialogue dialogue = CompiledDialogue.linear(
                DialogueId.parse("sample:greeting"), new DialogueNodeId("greeting"), lines);
        lines.add(line("Игрок", "Гулял."));

        assertEquals(1, dialogue.lines().size());
        assertThrows(UnsupportedOperationException.class, () -> dialogue.lines().add(line("Отец", "Понятно.")));
    }

    @Test
    void russianTextIsPreservedWithoutMojibake() {
        CompiledDialogueLine line = line("Отец", "Где ты был?");

        assertEquals("Отец", line.speakerDisplayName());
        assertEquals("Где ты был?", line.text());
    }

    private static CompiledDialogueLine line(String display, String text) {
        return new CompiledDialogueLine(
                new DialogueNodeId("greeting"), 0, SpeakerId.parse("sample:father"), display, text);
    }
}
