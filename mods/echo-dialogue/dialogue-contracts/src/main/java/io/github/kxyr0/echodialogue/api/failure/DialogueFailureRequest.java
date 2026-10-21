package io.github.kxyr0.echodialogue.api.failure;

import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import java.util.Objects;

public record DialogueFailureRequest(DialogueSessionId sessionId, int expectedRevision, String code) {
    public DialogueFailureRequest {
        Objects.requireNonNull(sessionId, "sessionId");
        code = Objects.requireNonNull(code, "code").trim();
        if (expectedRevision < 0 || !code.matches("[A-Za-z0-9_.:-]{1,128}")) {
            throw new IllegalArgumentException("Invalid dialogue execution failure");
        }
    }
}
