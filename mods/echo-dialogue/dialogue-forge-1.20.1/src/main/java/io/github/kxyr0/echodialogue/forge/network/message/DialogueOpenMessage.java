package io.github.kxyr0.echodialogue.forge.network.message;

import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.visual.SpeakerNameColor;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualId;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualKind;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentation;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Server-to-client current presentation.
 */
public record DialogueOpenMessage(
        String sessionId,
        int revision,
        Optional<SpeakerId> speakerId,
        DialoguePresentationKind kind,
        String speakerDisplayName,
        Optional<SpeakerNameColor> speakerNameColor,
        Optional<VisualPayload> visual,
        String text,
        boolean thought,
        List<CuePayload> cues,
        List<ChoicePayload> choices,
        boolean canAdvance,
        boolean canChoose,
        String dialogueId
) {
    public DialogueOpenMessage {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        speakerId = Objects.requireNonNull(speakerId, "speakerId");
        kind = Objects.requireNonNull(kind, "kind");
        speakerDisplayName = Objects.requireNonNull(speakerDisplayName, "speakerDisplayName");
        speakerNameColor = Objects.requireNonNull(speakerNameColor, "speakerNameColor");
        visual = Objects.requireNonNull(visual, "visual");
        text = Objects.requireNonNull(text, "text");
        cues = List.copyOf(Objects.requireNonNull(cues, "cues"));
        choices = List.copyOf(Objects.requireNonNull(choices, "choices"));
        dialogueId = requireText(dialogueId, "dialogueId");
        DialogueOpenMessageCodec.requireCueCount(cues.size());
        DialogueOpenMessageCodec.requireChoiceCount(choices.size());
    }

    public DialogueOpenMessage(
            String sessionId,
            int revision,
            DialoguePresentationKind kind,
            String speakerDisplayName,
            Optional<SpeakerNameColor> speakerNameColor,
            Optional<VisualPayload> visual,
            String text,
            List<ChoicePayload> choices,
            boolean canAdvance,
            boolean canChoose
    ) {
        this(sessionId, revision, Optional.empty(), kind, speakerDisplayName, speakerNameColor, visual, text, false,
                List.of(), choices, canAdvance, canChoose, "echo_dialogue:unknown");
    }

    public DialogueOpenMessage(
            String sessionId,
            int revision,
            String speakerDisplayName,
            String text,
            boolean canAdvance
    ) {
        this(sessionId, revision, Optional.empty(), DialoguePresentationKind.LINE, speakerDisplayName,
                Optional.empty(), Optional.empty(), text, false, List.of(), List.of(), canAdvance, false,
                "echo_dialogue:unknown");
    }

    public static void encode(DialogueOpenMessage message, FriendlyByteBuf buffer) {
        DialogueOpenMessageCodec.encode(message, buffer); }

    public static DialogueOpenMessage decode(FriendlyByteBuf buffer) { return DialogueOpenMessageCodec.decode(buffer); }

    public static DialogueOpenMessage from(DialoguePresentation presentation) {
        return new DialogueOpenMessage(
                presentation.sessionId().toString(),
                presentation.revision(),
                presentation.speakerId(),
                presentation.kind(),
                presentation.speakerDisplayName(),
                presentation.speakerNameColor(),
                presentation.speakerVisual()
                        .map(visual -> new VisualPayload(visual.kind().name(), visual.id().toString())),
                presentation.text(),
                presentation.thought(),
                presentation.cues().stream()
                        .map(cue -> new CuePayload(cue.name(), cue.value()))
                        .toList(),
                presentation.choices().stream()
                        .map(choice -> new ChoicePayload(choice.id().toString(), choice.text()))
                        .toList(),
                presentation.canAdvance(),
                presentation.canChoose(),
                presentation.dialogueId().toString());
    }

    public record CuePayload(String name, String value) {
        public CuePayload { name = requireText(name, "name"); value = requireText(value, "value"); }
    }

    public record ChoicePayload(String id, String text) {
        public ChoicePayload { id = requireText(id, "id"); text = requireText(text, "text"); }
    }

    public record VisualPayload(String kind, String id) {
        public VisualPayload {
            kind = Objects.requireNonNull(kind, "kind");
            id = Objects.requireNonNull(id, "id");
            SpeakerVisualKind.valueOf(kind);
            SpeakerVisualId.parse(id);
        }

        static Optional<VisualPayload> safe(String kind, String id) {
            try { return Optional.of(new VisualPayload(kind, id)); } catch (IllegalArgumentException exception) {
                return Optional.empty();
            }
        }
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name).trim();
        if (trimmed.isEmpty()) { throw new IllegalArgumentException(name + " must not be blank."); }
        return trimmed;
    }
}
