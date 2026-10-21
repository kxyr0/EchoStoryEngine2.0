package io.github.kxyr0.echodialogue.forge.client;

import io.github.kxyr0.echodialogue.forge.client.state.ClientDialogueState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.visual.SpeakerNameColor;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.forge.network.message.DialogueOpenMessage;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClientDialogueStateTest {
    @Test
    void waitingForServerBlocksDuplicateAdvanceUntilNewRevision() {
        ClientDialogueState state = new ClientDialogueState();
        state.open(open("session-1", 0, "Где ты был?"));

        assertTrue(state.requestAdvance().isPresent());
        assertTrue(state.requestAdvance().isEmpty());

        state.open(open("session-1", 0, "Где ты был?"));
        assertTrue(state.requestAdvance().isEmpty());

        state.open(open("session-1", 1, "Гулял."));
        assertEquals("Гулял.", state.current().orElseThrow().text());
        assertTrue(state.requestAdvance().isPresent());
    }

    @Test
    void replacementSessionCommitsWhileStalePacketsCannotOverwriteIt() {
        ClientDialogueState state = new ClientDialogueState();
        state.open(open("session-1", 1, "Гулял."));

        state.open(open("session-1", 0, "Где ты был?"));
        state.open(open("other-session", 2, "Чужая строка."));
        state.close("session-1", 0);

        assertEquals("Чужая строка.", state.current().orElseThrow().text());

        state.close("other-session", 2);
        assertTrue(state.current().isEmpty());
    }

    @Test
    void clearDropsPresentationAndWaitLock() {
        ClientDialogueState state = new ClientDialogueState();
        state.open(open("session-1", 0, "Где ты был?"));
        assertTrue(state.requestAdvance().isPresent());

        state.clear();
        state.open(open("session-2", 0, "Новая строка."));

        assertTrue(state.requestAdvance().isPresent());
    }

    @Test
    void requestChoiceAtUsesVisibleChoiceOrderAndWaitLock() {
        ClientDialogueState state = new ClientDialogueState();
        state.open(choiceOpen());

        ClientDialogueState.ChoiceRequest request = state.requestChoiceAt(1).orElseThrow();

        assertEquals("two", request.choiceId());
        assertTrue(state.requestChoiceAt(2).isEmpty());
    }

    @Test
    void dialogueLineUsesMessageSpeakerNameColor() {
        DialogueOpenMessage message = new DialogueOpenMessage("session-1", 0, DialoguePresentationKind.LINE,
                "Отец", Optional.of(SpeakerNameColor.parse("#8FD8FF")), Optional.empty(),
                "Где ты был?", List.of(), true, false);

        assertEquals(0x8FD8FF, ClientDialogueDraw.line(message, "Где ты был?")
                .getStyle().getColor().getValue());
    }

    @Test
    void thoughtLineDimsOnlyTheSpeakerVisual() {
        assertEquals(0.55F, ClientDialogueRenderer.visualAlpha(true, 1.0F), 0.0001F);
        assertEquals(0.4F, ClientDialogueRenderer.visualAlpha(false, 0.4F), 0.0001F);
    }

    private static DialogueOpenMessage open(String sessionId, int revision, String text) {
        return new DialogueOpenMessage(sessionId, revision, "Отец", text, true);
    }

    private static DialogueOpenMessage choiceOpen() {
        return new DialogueOpenMessage("session-1", 0, DialoguePresentationKind.CHOICE, "Отец", Optional.empty(),
                Optional.empty(),
                "Выбирай.",
                List.of(
                        new DialogueOpenMessage.ChoicePayload("one", "Первый"),
                        new DialogueOpenMessage.ChoicePayload("two", "Второй"),
                        new DialogueOpenMessage.ChoicePayload("three", "Третий")
                ), false, true);
    }
}
