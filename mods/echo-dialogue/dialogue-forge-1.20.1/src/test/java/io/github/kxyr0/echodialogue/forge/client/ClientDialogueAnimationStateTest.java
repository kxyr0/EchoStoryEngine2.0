package io.github.kxyr0.echodialogue.forge.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.forge.network.message.DialogueOpenMessage;
import org.junit.jupiter.api.Test;

class ClientDialogueAnimationStateTest {
    @Test
    void typewriterRevealsPresentationTextOverTime() {
        ClientDialogueAnimationState state = new ClientDialogueAnimationState();
        state.open(open("Where were you?"), 0);

        assertEquals(0, state.renderState(0).orElseThrow().text().visibleCharacters());
        assertEquals(0, state.renderState(ClientDialogueOverlayStyle.current().textAppearDelayMs())
                .orElseThrow().text().visibleCharacters());
        assertTrue(state.renderState(250).orElseThrow().text().visibleCharacters() > 0);
        assertEquals("Where were you?".length(), state.renderState(700).orElseThrow().text().visibleCharacters());
    }

    @Test
    void textFadeWaitsForItsOwnAppearTime() {
        ClientDialogueAnimationState state = new ClientDialogueAnimationState();
        state.open(open("Where were you?"), 0);

        assertEquals(0.0F, state.renderState(0).orElseThrow().textFade().alpha());
        assertEquals(0.0F, state.renderState(ClientDialogueOverlayStyle.current().textAppearDelayMs())
                .orElseThrow().textFade().alpha());
        assertTrue(state.renderState(160).orElseThrow().textFade().alpha() > 0.0F);
    }

    @Test
    void closeKeepsLastTypedTextAndClearsAfterFadeOut() {
        ClientDialogueAnimationState state = new ClientDialogueAnimationState();
        state.open(open("Where were you?"), 0);
        int visibleBeforeClose = state.renderState(300).orElseThrow().text().visibleCharacters();

        state.close(300);

        assertEquals(visibleBeforeClose, state.renderState(301).orElseThrow().text().visibleCharacters());
        assertTrue(state.renderState(301).orElseThrow().textFade().alpha() < 1.0F);
        assertTrue(state.renderState(500).isEmpty());
    }

    @Test
    void panelSizeInterpolatesTowardNewTarget() {
        ClientDialogueAnimationState state = new ClientDialogueAnimationState();
        state.open(open("Short."), 0);
        state.panelSize(100, 20, 0);

        ClientDialogueAnimationState.PanelSize resized = state.panelSize(200, 60, 1);

        assertTrue(resized.width() >= 100);
        assertTrue(resized.width() <= 200);
    }

    @Test
    void newPresentationResizesFromCurrentRenderedPanelSize() {
        ClientDialogueAnimationState state = new ClientDialogueAnimationState();
        state.open(open("Short.", 0), 0);
        state.panelSize(80, 20, 0);
        state.open(open("This line is deliberately much wider.", 1), 100);
        state.panelSize(240, 60, 100);
        state.panelSize(240, 60, 10_000);

        state.open(open("Tiny.", 2), 10_000);
        ClientDialogueAnimationState.PanelSize resized = state.panelSize(82, 20, 10_000);

        assertTrue(resized.width() > 200);
        assertTrue(resized.height() > 50);
    }

    private static DialogueOpenMessage open(String text) {
        return open(text, 0);
    }

    private static DialogueOpenMessage open(String text, int revision) {
        return new DialogueOpenMessage("session-1", revision, "Father", text, true);
    }
}
