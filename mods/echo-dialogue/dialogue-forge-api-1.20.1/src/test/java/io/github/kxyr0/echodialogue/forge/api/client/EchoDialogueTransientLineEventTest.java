package io.github.kxyr0.echodialogue.forge.api.client;

import io.github.kxyr0.echodialogue.forge.api.client.overlay.EchoDialogueTransientClearEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class EchoDialogueTransientLineEventTest {
    @Test
    void preservesBoundedTransientPresentation() {
        var event = new EchoDialogueTransientLineEvent("jdh:yard#window", "Наблюдение", "Окно открыто.", 5000L);
        assertEquals("jdh:yard#window", event.presentationId());
        assertEquals("Наблюдение", event.speakerDisplayName());
        assertEquals("Окно открыто.", event.text());
        assertEquals(5000L, event.durationMillis());
    }

    @Test
    void rejectsBlankTextAndUnboundedLifetime() {
        assertThrows(IllegalArgumentException.class,
                () -> new EchoDialogueTransientLineEvent("id", "speaker", " ", 5000L));
        assertThrows(IllegalArgumentException.class,
                () -> new EchoDialogueTransientLineEvent("id", "speaker", "text", 60_001L));
    }

    @Test
    void clearTargetsOnePresentation() {
        assertEquals("jdh:ambient/one", new EchoDialogueTransientClearEvent("jdh:ambient/one").presentationId());
        assertThrows(IllegalArgumentException.class, () -> new EchoDialogueTransientClearEvent(" "));
    }
}
