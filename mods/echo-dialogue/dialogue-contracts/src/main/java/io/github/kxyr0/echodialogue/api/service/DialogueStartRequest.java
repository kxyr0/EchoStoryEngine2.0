package io.github.kxyr0.echodialogue.api.service;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import java.util.Objects;

/**
 * Public request to start a dialogue with a caller-owned deterministic session id.
 */
public record DialogueStartRequest(
        DialogueSessionId sessionId,
        DialogueId dialogueId,
        AudienceId audienceId,
        java.util.Optional<Origin> origin
) {
    public DialogueStartRequest(DialogueSessionId sessionId, DialogueId dialogueId, AudienceId audienceId) {
        this(sessionId, dialogueId, audienceId, java.util.Optional.empty());
    }
    public DialogueStartRequest {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        dialogueId = Objects.requireNonNull(dialogueId, "dialogueId");
        audienceId = Objects.requireNonNull(audienceId, "audienceId");
        origin = Objects.requireNonNull(origin, "origin");
    }
    public record Origin(String rootExecution, String callerExecution) {
        public Origin {
            if (Objects.requireNonNull(rootExecution, "rootExecution").isBlank()
                    || Objects.requireNonNull(callerExecution, "callerExecution").isBlank()) {
                throw new IllegalArgumentException("Dialogue execution origin must not be blank");
            }
        }
    }
}
