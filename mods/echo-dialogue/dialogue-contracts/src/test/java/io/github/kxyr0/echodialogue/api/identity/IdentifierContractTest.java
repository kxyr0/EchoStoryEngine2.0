package io.github.kxyr0.echodialogue.api.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class IdentifierContractTest {
    @Test
    void dialogueIdRequiresNamespacedLowercaseValue() {
        DialogueId id = DialogueId.parse("sample:greeting");

        assertEquals("sample:greeting", id.toString());
        assertThrows(IllegalArgumentException.class, () -> DialogueId.parse("Sample:Greeting"));
        assertThrows(IllegalArgumentException.class, () -> DialogueId.parse("greeting"));
    }

    @Test
    void speakerIdUsesTheSameStableNamespaceRules() {
        SpeakerId id = SpeakerId.parse("sample:father");

        assertEquals("sample:father", id.toString());
        assertThrows(IllegalArgumentException.class, () -> SpeakerId.parse("father"));
    }
}
