package io.github.kxyr0.echodialogue.presentation.api;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.visual.SpeakerNameColor;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualRef;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable server-side context for resolving one speaker presentation.
 */
public record SpeakerPresentationRequest(
        DialogueSessionId sessionId,
        DialogueId dialogueId,
        SpeakerId speakerId,
        AudienceId audienceId,
        Kind kind,
        String authoredDisplayName,
        Optional<SpeakerNameColor> authoredNameColor,
        Optional<SpeakerVisualRef> authoredVisual
) {
    private static final int MAX_DISPLAY_NAME = 96;

    public SpeakerPresentationRequest {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        dialogueId = Objects.requireNonNull(dialogueId, "dialogueId");
        speakerId = Objects.requireNonNull(speakerId, "speakerId");
        audienceId = Objects.requireNonNull(audienceId, "audienceId");
        kind = Objects.requireNonNull(kind, "kind");
        authoredDisplayName = boundedDisplayName(authoredDisplayName);
        authoredNameColor = Objects.requireNonNull(authoredNameColor, "authoredNameColor");
        authoredVisual = Objects.requireNonNull(authoredVisual, "authoredVisual");
    }

    public SpeakerPresentationRequest(
            DialogueSessionId sessionId,
            DialogueId dialogueId,
            SpeakerId speakerId,
            AudienceId audienceId,
            Kind kind,
            String authoredDisplayName,
            Optional<SpeakerVisualRef> authoredVisual
    ) {
        this(sessionId, dialogueId, speakerId, audienceId, kind, authoredDisplayName, Optional.empty(),
                authoredVisual);
    }

    public enum Kind {
        LINE,
        CHOICE
    }

    private static String boundedDisplayName(String value) {
        String trimmed = Objects.requireNonNull(value, "authoredDisplayName").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Authored display name не должен быть пустым.");
        }
        if (trimmed.length() > MAX_DISPLAY_NAME) {
            throw new IllegalArgumentException("Authored display name слишком длинный.");
        }
        return trimmed;
    }
}
