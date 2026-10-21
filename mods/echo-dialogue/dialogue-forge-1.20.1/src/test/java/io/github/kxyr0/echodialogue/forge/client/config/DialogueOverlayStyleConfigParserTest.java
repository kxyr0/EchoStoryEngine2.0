package io.github.kxyr0.echodialogue.forge.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class DialogueOverlayStyleConfigParserTest {
    private final DialogueOverlayStyleConfigParser parser = new DialogueOverlayStyleConfigParser();

    @Test
    void parsesPartialDialogueOverlayStyleConfig() {
        DialogueOverlayStyleConfig parsed = parser.parse("""
                return DialogueOverlayStyle(
                    textScale: "1.25",
                    textShadow: false,
                    visualSize: 32,
                    visualTextOffsetY: 2,
                    visualOffsetX: -1,
                    visualOffsetY: 3,
                    backgroundColor: "0xA0101010"
                );
                """, DialogueOverlayStyleConfig.defaults()).orElseThrow();

        assertEquals(1.25F, parsed.textScale());
        assertTrue(!parsed.textShadow());
        assertEquals(32, parsed.visualSize());
        assertEquals(2, parsed.visualTextOffsetY());
        assertEquals(-1, parsed.visualOffsetX());
        assertEquals(3, parsed.visualOffsetY());
        assertEquals(0xA0101010, parsed.backgroundColor());
        assertEquals(DialogueOverlayStyleConfig.defaults().visualGap(), parsed.visualGap());
    }

    @Test
    void missingCallReturnsEmpty() {
        assertTrue(parser.parse("class Empty {}", DialogueOverlayStyleConfig.defaults()).isEmpty());
    }
}
