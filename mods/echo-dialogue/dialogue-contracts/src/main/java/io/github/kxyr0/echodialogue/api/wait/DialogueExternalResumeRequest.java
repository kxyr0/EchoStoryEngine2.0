package io.github.kxyr0.echodialogue.api.wait;

import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import java.util.Objects;

public record DialogueExternalResumeRequest(
        DialogueSessionId sessionId,
        int expectedRevision,
        String waitKey
) {
    public DialogueExternalResumeRequest {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        waitKey = Objects.requireNonNull(waitKey, "waitKey").trim();
        if (expectedRevision < 0) {
            throw new IllegalArgumentException("expectedRevision must not be negative.");
        }
        if (waitKey.isEmpty()) {
            throw new IllegalArgumentException("waitKey must not be blank.");
        }
    }
}
