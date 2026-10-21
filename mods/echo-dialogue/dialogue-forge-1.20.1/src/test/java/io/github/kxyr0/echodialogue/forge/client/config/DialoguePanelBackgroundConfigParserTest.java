package io.github.kxyr0.echodialogue.forge.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageRenderMode;
import io.github.kxyr0.echodialogue.forge.client.ui.settings.UiFillDirection;
import org.junit.jupiter.api.Test;

final class DialoguePanelBackgroundConfigParserTest {
    private final DialoguePanelBackgroundConfigParser parser = new DialoguePanelBackgroundConfigParser();

    @Test
    void parsesUiImagePanelBackgroundOverlay() {
        DialoguePanelBackgroundConfig parsed = parser.parse("""
                return DialoguePanelBackgroundOverlay(
                    mode: "tiled",
                    texture: "jdh:textures/echo_dialogue/gui/panel/test_dialogue_panel.png",
                    textureWidth: 29,
                    textureHeight: 28,
                    sliceLeft: 6,
                    sliceRight: 5,
                    sliceTop: 6,
                    sliceBottom: 8,
                    tileScaleX: "1.5",
                    tileScaleY: "2.0",
                    tileOriginX: 3,
                    tileOriginY: 4,
                    tileFullOnly: true,
                    fillDirection: "RIGHT_TO_LEFT",
                    fillAmount: "0.25"
                );
                """, DialoguePanelBackgroundConfig.defaults()).orElseThrow();

        assertTrue(parsed.enabled());
        assertEquals(UiImageRenderMode.TILED, parsed.mode());
        assertEquals(texture(), parsed.texture());
        assertEquals(29, parsed.sourceWidth());
        assertEquals(28, parsed.sourceHeight());
        assertEquals(6, parsed.borders().left());
        assertEquals(5, parsed.borders().right());
        assertEquals(1.5F, parsed.tileSettings().scaleX());
        assertEquals(2.0F, parsed.tileSettings().scaleY());
        assertEquals(3, parsed.tileSettings().originX());
        assertEquals(4, parsed.tileSettings().originY());
        assertTrue(parsed.tileSettings().fullOnly());
        assertEquals(UiFillDirection.RIGHT_TO_LEFT, parsed.fillSettings().direction());
        assertEquals(0.25F, parsed.fillSettings().amount());
    }

    @Test
    void missingCallReturnsEmpty() {
        assertTrue(parser.parse("class Empty {}", DialoguePanelBackgroundConfig.defaults()).isEmpty());
    }

    @Test
    void parsesCustomOverlayCallName() {
        DialoguePanelBackgroundConfigParser choiceParser =
                new DialoguePanelBackgroundConfigParser("DialogueChoiceBackgroundOverlay");
        DialoguePanelBackgroundConfig parsed = choiceParser.parse("""
                return DialogueChoiceBackgroundOverlay(
                    mode: "sliced",
                    texture: "jdh:textures/echo_dialogue/gui/panel/dialogue_choice.png",
                    textureWidth: 29,
                    textureHeight: 28,
                    sliceLeft: 11,
                    sliceRight: 10,
                    sliceTop: 5,
                    sliceBottom: 8
                );
                """, DialoguePanelBackgroundConfig.defaults()).orElseThrow();

        assertEquals(UiImageRenderMode.SLICED, parsed.mode());
        assertEquals("jdh:textures/echo_dialogue/gui/panel/dialogue_choice.png", parsed.texture());
        assertEquals(11, parsed.borders().left());
        assertEquals(10, parsed.borders().right());
    }

    private static String texture() {
        return "jdh:textures/echo_dialogue/gui/panel/test_dialogue_panel.png";
    }
}
