package io.github.kxyr0.echodialogue.forge.network.message;

import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.visual.SpeakerNameColor;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Wire codec for the clientbound dialogue-open packet.
 */
final class DialogueOpenMessageCodec {
    static final int MAX_CHOICES = 4;
    static final int MAX_CUES = 8;
    private static final int MAX_SESSION_ID = 256;
    private static final int MAX_SPEAKER_ID = 128;
    private static final int MAX_SPEAKER = 96;
    private static final int MAX_TEXT = 2048;
    private static final int MAX_CHOICE_ID = 64;
    private static final int MAX_CHOICE_TEXT = 512;
    private static final int MAX_VISUAL_KIND = 16;
    private static final int MAX_VISUAL_ID = 128;
    private static final int MAX_CUE_NAME = 48;
    private static final int MAX_CUE_VALUE = 128;
    private static final int MAX_DIALOGUE_ID = 256;

    private DialogueOpenMessageCodec() {
    }

    static void encode(DialogueOpenMessage message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.sessionId(), MAX_SESSION_ID);
        buffer.writeVarInt(message.revision());
        writeSpeakerId(message, buffer);
        buffer.writeEnum(message.kind());
        buffer.writeUtf(message.speakerDisplayName(), MAX_SPEAKER);
        writeSpeakerNameColor(message, buffer);
        writeVisual(message, buffer);
        buffer.writeUtf(message.text(), MAX_TEXT);
        buffer.writeBoolean(message.thought());
        writeCues(message, buffer);
        writeChoices(message, buffer);
        buffer.writeBoolean(message.canAdvance());
        buffer.writeBoolean(message.canChoose());
        buffer.writeUtf(message.dialogueId(), MAX_DIALOGUE_ID);
    }

    static DialogueOpenMessage decode(FriendlyByteBuf buffer) {
        String sessionId = buffer.readUtf(MAX_SESSION_ID);
        int revision = buffer.readVarInt();
        Optional<SpeakerId> speakerId = readSpeakerId(buffer);
        DialoguePresentationKind kind = buffer.readEnum(DialoguePresentationKind.class);
        String speaker = buffer.readUtf(MAX_SPEAKER);
        Optional<SpeakerNameColor> speakerNameColor = readSpeakerNameColor(buffer);
        Optional<DialogueOpenMessage.VisualPayload> visual = readVisual(buffer);
        String text = buffer.readUtf(MAX_TEXT);
        boolean thought = buffer.readBoolean();
        List<DialogueOpenMessage.CuePayload> cues = readCues(buffer);
        List<DialogueOpenMessage.ChoicePayload> choices = readChoices(buffer);
        boolean canAdvance = buffer.readBoolean();
        boolean canChoose = buffer.readBoolean();
        String dialogueId = buffer.readUtf(MAX_DIALOGUE_ID);
        return new DialogueOpenMessage(sessionId, revision, speakerId, kind, speaker, speakerNameColor, visual, text,
                thought, cues, choices, canAdvance, canChoose, dialogueId);
    }

    static void requireCueCount(int count) {
        requireCount(count, MAX_CUES, "Cue");
    }

    static void requireChoiceCount(int count) {
        requireCount(count, MAX_CHOICES, "Choice");
    }

    private static void writeSpeakerId(DialogueOpenMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.speakerId().isPresent());
        message.speakerId().ifPresent(id -> buffer.writeUtf(id.toString(), MAX_SPEAKER_ID));
    }

    private static void writeSpeakerNameColor(DialogueOpenMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.speakerNameColor().isPresent());
        message.speakerNameColor().ifPresent(color -> buffer.writeVarInt(color.rgb()));
    }

    private static void writeVisual(DialogueOpenMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.visual().isPresent());
        message.visual().ifPresent(visual -> {
            buffer.writeUtf(visual.kind(), MAX_VISUAL_KIND);
            buffer.writeUtf(visual.id(), MAX_VISUAL_ID);
        });
    }

    private static void writeCues(DialogueOpenMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.cues().size());
        message.cues().forEach(cue -> {
            buffer.writeUtf(cue.name(), MAX_CUE_NAME);
            buffer.writeUtf(cue.value(), MAX_CUE_VALUE);
        });
    }

    private static void writeChoices(DialogueOpenMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.choices().size());
        message.choices().forEach(choice -> {
            buffer.writeUtf(choice.id(), MAX_CHOICE_ID);
            buffer.writeUtf(choice.text(), MAX_CHOICE_TEXT);
        });
    }

    private static Optional<SpeakerId> readSpeakerId(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return Optional.empty();
        }
        return Optional.of(SpeakerId.parse(buffer.readUtf(MAX_SPEAKER_ID)));
    }

    private static Optional<SpeakerNameColor> readSpeakerNameColor(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return Optional.empty();
        }
        return Optional.of(SpeakerNameColor.rgb(buffer.readVarInt()));
    }

    private static Optional<DialogueOpenMessage.VisualPayload> readVisual(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return Optional.empty();
        }
        return DialogueOpenMessage.VisualPayload.safe(buffer.readUtf(MAX_VISUAL_KIND), buffer.readUtf(MAX_VISUAL_ID));
    }

    private static List<DialogueOpenMessage.CuePayload> readCues(FriendlyByteBuf buffer) {
        int cueCount = buffer.readVarInt();
        if (cueCount < 0 || cueCount > MAX_CUES) {
            throw new IllegalArgumentException("Cue count is outside packet bounds.");
        }
        List<DialogueOpenMessage.CuePayload> cues = new ArrayList<>();
        for (int index = 0; index < cueCount; index++) {
            cues.add(new DialogueOpenMessage.CuePayload(
                    buffer.readUtf(MAX_CUE_NAME), buffer.readUtf(MAX_CUE_VALUE)));
        }
        return cues;
    }

    private static List<DialogueOpenMessage.ChoicePayload> readChoices(FriendlyByteBuf buffer) {
        int choiceCount = buffer.readVarInt();
        if (choiceCount < 0 || choiceCount > MAX_CHOICES) {
            throw new IllegalArgumentException("Choice count is outside packet bounds.");
        }
        List<DialogueOpenMessage.ChoicePayload> choices = new ArrayList<>();
        for (int index = 0; index < choiceCount; index++) {
            choices.add(new DialogueOpenMessage.ChoicePayload(
                    buffer.readUtf(MAX_CHOICE_ID), buffer.readUtf(MAX_CHOICE_TEXT)));
        }
        return choices;
    }

    private static void requireCount(int count, int max, String name) {
        if (count > max) {
            throw new IllegalArgumentException(name + " count is outside packet bounds.");
        }
    }
}
