package io.github.kxyr0.echodialogue.forge.api.client;

import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Public client read model for one committed Line presentation. */
public record DialogueLinePresentationSnapshot(
        DialogueSessionId sessionId,
        DialogueId dialogueId,
        int revision,
        Optional<SpeakerId> speakerId,
        List<DialogueLineCue> cues
) {
    public DialogueLinePresentationSnapshot {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        dialogueId = Objects.requireNonNull(dialogueId, "dialogueId");
        speakerId = Objects.requireNonNull(speakerId, "speakerId");
        cues = List.copyOf(Objects.requireNonNull(cues, "cues"));
        if (revision < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }
    }
}
