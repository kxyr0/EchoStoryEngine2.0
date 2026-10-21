package io.github.kxyr0.echodialogue.api.wait;

import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import java.util.Objects;

public record DialogueExternalWaitRequest(
        DialogueSessionId sessionId,
        int expectedRevision,
        DialogueExternalWait externalWait
) {
    public DialogueExternalWaitRequest {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        externalWait = Objects.requireNonNull(externalWait, "externalWait");
        if (expectedRevision < 0) {
            throw new IllegalArgumentException("expectedRevision must not be negative.");
        }
    }
}
