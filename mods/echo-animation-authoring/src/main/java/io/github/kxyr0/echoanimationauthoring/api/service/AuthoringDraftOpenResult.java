package io.github.kxyr0.echoanimationauthoring.api.service;

import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringDraftView;
import java.util.Objects;
import java.util.Optional;

public record AuthoringDraftOpenResult(Status status, Optional<AuthoringDraftView> draft, String message) {
    public AuthoringDraftOpenResult {
        status = Objects.requireNonNull(status, "status");
        draft = Objects.requireNonNull(draft, "draft");
        message = Objects.requireNonNull(message, "message");
    }

    public enum Status {
        OPENED,
        WORKSPACE_NOT_CONFIGURED,
        INVALID_REQUEST,
        FAILURE
    }
}
