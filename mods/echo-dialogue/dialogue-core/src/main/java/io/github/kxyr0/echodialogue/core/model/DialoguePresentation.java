package io.github.kxyr0.echodialogue.core.model;

import io.github.kxyr0.echodialogue.api.choice.ChoiceId;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.visual.SpeakerNameColor;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualRef;
import io.github.kxyr0.echodialogue.core.model.validation.DialoguePresentationValidation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Client-safe read model for one active dialogue presentation.
 */
public record DialoguePresentation(
        DialogueSessionId sessionId,
        DialogueId dialogueId,
        AudienceId audienceId,
        Optional<SpeakerId> speakerId,
        int revision,
        DialoguePresentationKind kind,
        String speakerDisplayName,
        Optional<SpeakerNameColor> speakerNameColor,
        Optional<SpeakerVisualRef> speakerVisual,
        String text,
        boolean thought,
        List<Cue> cues,
        List<VisibleChoice> choices,
        boolean canAdvance,
        boolean canChoose
) {
    public DialoguePresentation {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        dialogueId = Objects.requireNonNull(dialogueId, "dialogueId");
        audienceId = Objects.requireNonNull(audienceId, "audienceId");
        speakerId = Objects.requireNonNull(speakerId, "speakerId");
        kind = Objects.requireNonNull(kind, "kind");
        speakerNameColor = Objects.requireNonNull(speakerNameColor, "speakerNameColor");
        speakerVisual = Objects.requireNonNull(speakerVisual, "speakerVisual");
        cues = List.copyOf(Objects.requireNonNull(cues, "cues"));
        choices = List.copyOf(Objects.requireNonNull(choices, "choices"));
        DialoguePresentationValidation.validate(revision, kind, choices);
        speakerDisplayName = DialoguePresentationValidation.lineText(
                speakerDisplayName, kind, "speakerDisplayName");
        text = DialoguePresentationValidation.lineText(text, kind, "text");
        DialoguePresentationValidation.validateSpeakerContext(speakerDisplayName, text,
                speakerNameColor.isPresent() || speakerVisual.isPresent());
    }

    public DialoguePresentation(
            DialogueSessionId sessionId,
            DialogueId dialogueId,
            AudienceId audienceId,
            SpeakerId speakerId,
            int revision,
            String speakerDisplayName,
            String text,
            boolean canAdvance
    ) {
        this(sessionId, dialogueId, audienceId, Optional.of(speakerId), revision, DialoguePresentationKind.LINE,
                speakerDisplayName, Optional.empty(), Optional.empty(), text, false, List.of(), List.of(), canAdvance,
                false);
    }

    public DialoguePresentation(
            DialogueSessionId sessionId,
            DialogueId dialogueId,
            AudienceId audienceId,
            SpeakerId speakerId,
            int revision,
            String speakerDisplayName,
            Optional<SpeakerNameColor> speakerNameColor,
            Optional<SpeakerVisualRef> speakerVisual,
            String text,
            boolean canAdvance
    ) {
        this(sessionId, dialogueId, audienceId, Optional.of(speakerId), revision, DialoguePresentationKind.LINE,
                speakerDisplayName, speakerNameColor, speakerVisual, text, false, List.of(), List.of(), canAdvance,
                false);
    }

    public DialoguePresentation(
            DialogueSessionId sessionId,
            DialogueId dialogueId,
            AudienceId audienceId,
            SpeakerId speakerId,
            int revision,
            String speakerDisplayName,
            Optional<SpeakerVisualRef> speakerVisual,
            String text,
            boolean canAdvance
    ) {
        this(sessionId, dialogueId, audienceId, Optional.of(speakerId), revision, DialoguePresentationKind.LINE,
                speakerDisplayName, Optional.empty(), speakerVisual, text, false, List.of(), List.of(), canAdvance,
                false);
    }

    public static DialoguePresentation line(Context context, int revision, Prompt prompt, boolean canAdvance) {
        return new DialoguePresentation(context.sessionId(), context.dialogueId(), context.audienceId(),
                Optional.of(prompt.speakerId()), revision, DialoguePresentationKind.LINE,
                prompt.speakerDisplayName(), prompt.speakerNameColor(), prompt.speakerVisual(), prompt.text(),
                prompt.thought(), prompt.cues(), List.of(), canAdvance, false);
    }

    public static DialoguePresentation choice(Context context, int revision, List<VisibleChoice> choices) {
        return new DialoguePresentation(context.sessionId(), context.dialogueId(), context.audienceId(),
                Optional.empty(), revision,
                DialoguePresentationKind.CHOICE, "", Optional.empty(), Optional.empty(), "", false, List.of(), choices,
                false, true);
    }

    public static DialoguePresentation choice(
            Context context,
            int revision,
            Prompt prompt,
            List<VisibleChoice> choices
    ) {
        return new DialoguePresentation(context.sessionId(), context.dialogueId(), context.audienceId(),
                Optional.of(prompt.speakerId()), revision,
                DialoguePresentationKind.CHOICE, prompt.speakerDisplayName(), prompt.speakerNameColor(),
                prompt.speakerVisual(), prompt.text(), prompt.thought(), prompt.cues(), choices, false, true);
    }

    public DialoguePresentation withSpeakerPresentation(
            String displayName,
            Optional<SpeakerNameColor> nameColor,
            Optional<SpeakerVisualRef> visual
    ) {
        return new DialoguePresentation(sessionId, dialogueId, audienceId, speakerId, revision, kind, displayName,
                nameColor, visual, text, thought, cues, choices, canAdvance, canChoose);
    }

    public record Cue(String name, String value) {
        public Cue {
            name = requireText(name, "name");
            value = requireText(value, "value");
        }
    }

    public record VisibleChoice(ChoiceId id, String text) {
        public VisibleChoice {
            id = Objects.requireNonNull(id, "id");
            text = requireText(text, "text");
        }
    }

    public record Context(DialogueSessionId sessionId, DialogueId dialogueId, AudienceId audienceId) {
        public Context {
            sessionId = Objects.requireNonNull(sessionId, "sessionId");
            dialogueId = Objects.requireNonNull(dialogueId, "dialogueId");
            audienceId = Objects.requireNonNull(audienceId, "audienceId");
        }
    }

    public record Prompt(
            SpeakerId speakerId,
            String speakerDisplayName,
            Optional<SpeakerNameColor> speakerNameColor,
            Optional<SpeakerVisualRef> speakerVisual,
            String text,
            boolean thought,
            List<Cue> cues
    ) {
        public Prompt {
            speakerId = Objects.requireNonNull(speakerId, "speakerId");
            speakerDisplayName = Objects.requireNonNull(speakerDisplayName, "speakerDisplayName");
            speakerNameColor = Objects.requireNonNull(speakerNameColor, "speakerNameColor");
            speakerVisual = Objects.requireNonNull(speakerVisual, "speakerVisual");
            text = Objects.requireNonNull(text, "text");
            cues = List.copyOf(Objects.requireNonNull(cues, "cues"));
        }

        public Prompt(
                SpeakerId speakerId,
                String speakerDisplayName,
                Optional<SpeakerVisualRef> speakerVisual,
                String text
        ) {
            this(speakerId, speakerDisplayName, Optional.empty(), speakerVisual, text, false, List.of());
        }
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name).trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return trimmed;
    }
}
