package io.github.kxyr0.echodialogue.core.session;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.outcome.DialogueOutcomeId;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable session cursor owned by the server-side host.
 */
public record DialogueSessionState(
        DialogueSessionId sessionId,
        DialogueId dialogueId,
        AudienceId audienceId,
        DialogueNodeId currentNodeId,
        int lineOrdinal,
        DialoguePresentationKind presentationKind,
        DialogueSessionPhase phase,
        int revision,
        Optional<DialogueOutcomeId> outcomeId,
        Optional<DialogueExternalWait> externalWait
) {
    public DialogueSessionState {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        dialogueId = Objects.requireNonNull(dialogueId, "dialogueId");
        audienceId = Objects.requireNonNull(audienceId, "audienceId");
        currentNodeId = Objects.requireNonNull(currentNodeId, "currentNodeId");
        presentationKind = Objects.requireNonNull(presentationKind, "presentationKind");
        phase = Objects.requireNonNull(phase, "phase");
        outcomeId = Objects.requireNonNull(outcomeId, "outcomeId");
        externalWait = Objects.requireNonNull(externalWait, "externalWait");
        if (lineOrdinal < 0 || revision < 0) {
            throw new IllegalArgumentException("Line index and revision must not be negative.");
        }
        if (phase == DialogueSessionPhase.WAITING && externalWait.isEmpty()) {
            throw new IllegalArgumentException("WAITING session requires externalWait.");
        }
    }

    public DialogueSessionState(
            DialogueSessionId sessionId,
            DialogueId dialogueId,
            AudienceId audienceId,
            DialogueNodeId currentNodeId,
            int lineOrdinal,
            DialoguePresentationKind presentationKind,
            DialogueSessionPhase phase,
            int revision,
            Optional<DialogueOutcomeId> outcomeId
    ) {
        this(sessionId, dialogueId, audienceId, currentNodeId, lineOrdinal, presentationKind, phase, revision,
                outcomeId, Optional.empty());
    }

    public int lineIndex() {
        return lineOrdinal;
    }

    public DialogueSessionState withPresentation(
            DialogueNodeId nextNodeId,
            int nextLineOrdinal,
            DialoguePresentationKind nextKind
    ) {
        return new DialogueSessionState(sessionId, dialogueId, audienceId, nextNodeId, nextLineOrdinal,
                nextKind, DialogueSessionPhase.ACTIVE, revision + 1, Optional.empty(), Optional.empty());
    }

    public DialogueSessionState waiting(DialogueExternalWait wait) {
        return new DialogueSessionState(sessionId, dialogueId, audienceId, currentNodeId, lineOrdinal,
                presentationKind, DialogueSessionPhase.WAITING, revision + 1, Optional.empty(), Optional.of(wait));
    }

    public DialogueSessionState resumed() {
        return new DialogueSessionState(sessionId, dialogueId, audienceId, currentNodeId, lineOrdinal,
                presentationKind, DialogueSessionPhase.ACTIVE, revision + 1, Optional.empty(), Optional.empty());
    }

    public DialogueSessionState completed(DialogueOutcomeId outcome) {
        return new DialogueSessionState(sessionId, dialogueId, audienceId, currentNodeId, lineOrdinal,
                presentationKind, DialogueSessionPhase.COMPLETED, revision + 1, Optional.of(outcome),
                Optional.empty());
    }

    public DialogueSessionState cancelled() {
        return new DialogueSessionState(sessionId, dialogueId, audienceId, currentNodeId, lineOrdinal,
                presentationKind, DialogueSessionPhase.CANCELLED, revision + 1, outcomeId, Optional.empty());
    }
}
