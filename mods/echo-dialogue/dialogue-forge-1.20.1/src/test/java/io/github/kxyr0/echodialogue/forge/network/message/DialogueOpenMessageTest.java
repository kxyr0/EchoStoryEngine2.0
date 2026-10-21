package io.github.kxyr0.echodialogue.forge.network.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.choice.ChoiceId;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.visual.SpeakerNameColor;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualRef;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentation;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class DialogueOpenMessageTest {
    @Test
    void roundTripsLineVisualDescriptor() {
        DialogueOpenMessage message = DialogueOpenMessage.from(new DialoguePresentation(
                new DialogueSessionId("session-1"),
                DialogueId.parse("sample:greeting"),
                new AudienceId("audience-1"),
                Optional.of(SpeakerId.parse("sample:father")),
                1,
                DialoguePresentationKind.LINE,
                "Отец",
                Optional.of(SpeakerNameColor.parse("#8FD8FF")),
                Optional.of(SpeakerVisualRef.skinHead("sample:father")),
                "Где ты был?",
                true,
                List.of(new DialoguePresentation.Cue("faceAnimation", "sample:look_left")),
                List.of(),
                true,
                false));

        DialogueOpenMessage decoded = roundTrip(message);

        assertEquals(Optional.of(SpeakerId.parse("sample:father")), decoded.speakerId());
        assertEquals("#8FD8FF", decoded.speakerNameColor().orElseThrow().hex());
        assertEquals("SKIN_HEAD", decoded.visual().orElseThrow().kind());
        assertEquals("sample:father", decoded.visual().orElseThrow().id());
        assertTrue(decoded.thought());
        assertEquals(new DialogueOpenMessage.CuePayload("faceAnimation", "sample:look_left"),
                decoded.cues().get(0));
        assertEquals("Где ты был?", decoded.text());
    }

    @Test
    void roundTripsStoryActionSessionId() {
        String sessionId = "story-action/jdh:manual_chapter_1_dialogue_test_ms5umb62/load_dialogue";
        DialogueOpenMessage message = new DialogueOpenMessage(sessionId, 1, "John", "test", true);

        DialogueOpenMessage decoded = roundTrip(message);

        assertEquals(sessionId, decoded.sessionId());
    }

    @Test
    void roundTripsChoicePromptVisualDescriptor() {
        DialogueOpenMessage message = DialogueOpenMessage.from(new DialoguePresentation(
                new DialogueSessionId("session-1"),
                DialogueId.parse("sample:greeting"),
                new AudienceId("audience-1"),
                Optional.of(SpeakerId.parse("sample:merchant")),
                2,
                DialoguePresentationKind.CHOICE,
                "Торговец",
                Optional.empty(),
                Optional.of(SpeakerVisualRef.portrait("sample:merchant")),
                "Выбирай.",
                false,
                List.of(),
                List.of(new DialoguePresentation.VisibleChoice(
                        new ChoiceId("accept"), "Да.")),
                false,
                true));

        DialogueOpenMessage decoded = roundTrip(message);

        assertEquals(Optional.of(SpeakerId.parse("sample:merchant")), decoded.speakerId());
        assertEquals("PORTRAIT", decoded.visual().orElseThrow().kind());
        assertEquals("sample:merchant", decoded.visual().orElseThrow().id());
        assertEquals("accept", decoded.choices().get(0).id());
    }

    @Test
    void invalidVisualPayloadConstructorRejectsRawPath() {
        assertThrows(IllegalArgumentException.class,
                () -> new DialogueOpenMessage.VisualPayload("PORTRAIT", "sample:portrait.png"));
    }

    @Test
    void decodeDropsInvalidVisualButKeepsReadableText() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeUtf("session-1");
        buffer.writeVarInt(1);
        buffer.writeBoolean(false);
        buffer.writeEnum(DialoguePresentationKind.LINE);
        buffer.writeUtf("Отец");
        buffer.writeBoolean(false);
        buffer.writeBoolean(true);
        buffer.writeUtf("WRONG");
        buffer.writeUtf("sample:father");
        buffer.writeUtf("Где ты был?");
        buffer.writeBoolean(false);
        buffer.writeVarInt(0);
        buffer.writeVarInt(0);
        buffer.writeBoolean(true);
        buffer.writeBoolean(false);
        buffer.writeUtf("sample:test");

        DialogueOpenMessage decoded = DialogueOpenMessage.decode(buffer);

        assertTrue(decoded.visual().isEmpty());
        assertEquals("Где ты был?", decoded.text());
    }

    @Test
    void decodeRejectsChoiceCountOutsidePacketBounds() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeUtf("session-1");
        buffer.writeVarInt(1);
        buffer.writeBoolean(false);
        buffer.writeEnum(DialoguePresentationKind.CHOICE);
        buffer.writeUtf("Отец");
        buffer.writeBoolean(false);
        buffer.writeBoolean(false);
        buffer.writeUtf("Выбирай.");
        buffer.writeBoolean(false);
        buffer.writeVarInt(0);
        buffer.writeVarInt(5);

        assertThrows(IllegalArgumentException.class, () -> DialogueOpenMessage.decode(buffer));
    }

    @Test
    void decodeRejectsCueCountOutsidePacketBounds() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeUtf("session-1");
        buffer.writeVarInt(1);
        buffer.writeBoolean(false);
        buffer.writeEnum(DialoguePresentationKind.LINE);
        buffer.writeUtf("Отец");
        buffer.writeBoolean(false);
        buffer.writeBoolean(false);
        buffer.writeUtf("Где ты был?");
        buffer.writeBoolean(false);
        buffer.writeVarInt(9);

        assertThrows(IllegalArgumentException.class, () -> DialogueOpenMessage.decode(buffer));
    }

    private static DialogueOpenMessage roundTrip(DialogueOpenMessage message) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        DialogueOpenMessage.encode(message, buffer);
        return DialogueOpenMessage.decode(buffer);
    }
}
