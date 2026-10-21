package io.github.kxyr0.echoanimationauthoring.api.service;

import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringDraftView;
import java.util.Objects;
import java.util.Optional;

public record AuthoringDraftMutationResult(Status status, Optional<AuthoringDraftView> draft, String message) {
    public AuthoringDraftMutationResult {
        status = Objects.requireNonNull(status, "status");
        draft = Objects.requireNonNull(draft, "draft");
        message = Objects.requireNonNull(message, "message");
    }

    public enum Status {
        APPLIED,
        DRAFT_NOT_FOUND,
        INVALID_MUTATION,
        FAILURE
    }
}
